package com.wambe.scanner;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ScanService {

    private final ClamAvClient clamAv;
    private final JsonMapper jsonMapper;
    private final RestClient callbackClient;
    private final HttpClient httpClient;
    private final ScannerDestinationPolicy destinationPolicy;
    private final byte[] secret;
    private final int maxBytes;

    public ScanService(
            ClamAvClient clamAv,
            JsonMapper jsonMapper,
            RestClient.Builder restClientBuilder,
            ScannerDestinationPolicy destinationPolicy,
            @Value("${wambe.scanner.hmac-secret}") String secret,
            @Value("${wambe.scanner.max-bytes}") int maxBytes) {
        this.clamAv = clamAv;
        this.jsonMapper = jsonMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        var callbackHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        var callbackRequestFactory = new JdkClientHttpRequestFactory(callbackHttpClient);
        callbackRequestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.callbackClient = restClientBuilder
                .requestFactory(callbackRequestFactory)
                .build();
        this.destinationPolicy = destinationPolicy;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.maxBytes = maxBytes;
    }

    public void scan(ScanRequest request) {
        destinationPolicy.validate(request);
        ScanResult result;
        try {
            byte[] content = download(request);
            String detectedType = detectType(content);
            String digest = DispatchHmacFilter.sha256(content);
            if ("application/octet-stream".equals(detectedType)) {
                result = ScanResult.rejected(detectedType, digest, "unsupported_type");
            } else if (!detectedType.equals(request.claimedMimeType())) {
                result = ScanResult.rejected(detectedType, digest, "type_mismatch");
            } else {
                result = switch (clamAv.scan(content)) {
                    case INFECTED -> ScanResult.rejected(detectedType, digest, "malware_detected");
                    case ERROR -> ScanResult.rejected(detectedType, digest, "scan_failed");
                    case CLEAN -> clean(request, content, detectedType, digest);
                };
            }
        } catch (OutboundRedirectException exception) {
            throw exception;
        } catch (Exception exception) {
            result = ScanResult.rejected(
                    "application/octet-stream",
                    "0000000000000000000000000000000000000000000000000000000000000000",
                    "scan_failed");
        }
        callback(request, result);
    }

    private ScanResult clean(
            ScanRequest request,
            byte[] content,
            String detectedType,
            String digest) throws Exception {
        if (detectedType.startsWith("image/")) {
            HttpRequest upload = HttpRequest.newBuilder(request.previewWriteUrl())
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", detectedType)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();
            HttpResponse<Void> response = httpClient.send(upload, HttpResponse.BodyHandlers.discarding());
            if (isRedirect(response.statusCode())) {
                throw new OutboundRedirectException();
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ScanResult.rejected(detectedType, digest, "scan_failed");
            }
            return ScanResult.clean(detectedType, digest, request.previewQuarantinePath(), digest);
        }
        return ScanResult.clean(detectedType, digest, null, null);
    }

    private byte[] download(ScanRequest request) throws Exception {
        HttpRequest download = HttpRequest.newBuilder(request.readUrl())
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<InputStream> response =
                httpClient.send(download, HttpResponse.BodyHandlers.ofInputStream());
        if (isRedirect(response.statusCode())) {
            response.body().close();
            throw new OutboundRedirectException();
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IllegalStateException("Storage download failed");
        }
        try (InputStream input = response.body()) {
            byte[] content = input.readNBytes(maxBytes + 1);
            if (content.length == 0 || content.length > maxBytes) {
                throw new IllegalArgumentException("Invalid object size");
            }
            return content;
        }
    }

    private String detectType(byte[] content) {
        if (startsWith(content, 0xff, 0xd8, 0xff)) {
            return "image/jpeg";
        }
        if (startsWith(content, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) {
            return "image/png";
        }
        if (content.length >= 12
                && new String(content, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(content, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return "image/webp";
        }
        if (content.length >= 5
                && new String(content, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    private boolean startsWith(byte[] content, int... expected) {
        if (content.length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if ((content[index] & 0xff) != expected[index]) {
                return false;
            }
        }
        return true;
    }

    private void callback(ScanRequest request, ScanResult result) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("mediaId", request.mediaId());
            body.put("result", result.clean() ? "clean" : "rejected");
            body.put("detectedMimeType", result.detectedMimeType());
            body.put("objectSha256", result.objectSha256());
            if (!result.clean()) {
                body.put("rejectionCode", result.rejectionCode());
            }
            if (result.previewQuarantinePath() != null) {
                body.put("previewQuarantinePath", result.previewQuarantinePath());
                body.put("previewSha256", result.previewSha256());
            }
            byte[] json = jsonMapper.writeValueAsBytes(body);
            String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
            UUID nonce = UUID.randomUUID();
            String digest = DispatchHmacFilter.sha256(json);
            String signature = hmac(timestamp + "\n" + nonce + "\n" + digest);
            var response = callbackClient.post()
                    .uri(request.callbackUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(json.length)
                    .header("X-Wambe-Timestamp", timestamp)
                    .header("X-Wambe-Nonce", nonce.toString())
                    .header("X-Wambe-Signature", signature)
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
            if (response.getStatusCode().is3xxRedirection()) {
                throw new OutboundRedirectException();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Scanner callback failed", exception);
        }
    }

    private String hmac(String value) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret, "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(
                    mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private boolean isRedirect(int statusCode) {
        return statusCode >= 300 && statusCode < 400;
    }

    private static final class OutboundRedirectException extends RuntimeException {
    }

    private record ScanResult(
            boolean clean,
            String detectedMimeType,
            String objectSha256,
            String rejectionCode,
            String previewQuarantinePath,
            String previewSha256) {

        static ScanResult clean(
                String detectedMimeType,
                String objectSha256,
                String previewQuarantinePath,
                String previewSha256) {
            return new ScanResult(
                    true,
                    detectedMimeType,
                    objectSha256,
                    null,
                    previewQuarantinePath,
                    previewSha256);
        }

        static ScanResult rejected(String detectedMimeType, String objectSha256, String rejectionCode) {
            return new ScanResult(false, detectedMimeType, objectSha256, rejectionCode, null, null);
        }
    }
}

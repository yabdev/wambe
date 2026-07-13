package com.wambe.api.integration.scanner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wambe.api.integration.storage.ObjectStoragePort;
import com.wambe.api.media.persistence.MediaEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpScannerDispatchAdapter implements ScannerDispatchPort {

    private final RestClient client;
    private final ObjectStoragePort storage;
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final String apiBaseUrl;

    public HttpScannerDispatchAdapter(
            RestClient.Builder builder,
            ObjectStoragePort storage,
            ObjectMapper objectMapper,
            @Value("${wambe.scanner.url}") String scannerUrl,
            @Value("${wambe.scanner.hmac-secret}") String secret,
            @Value("${wambe.api-base-url}") String apiBaseUrl) {
        this.client = builder.baseUrl(scannerUrl).build();
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.apiBaseUrl = apiBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void dispatch(UUID jobId, MediaEntity media) {
        UUID nonce = UUID.randomUUID();
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
        Map<String, Object> body = Map.of(
                "jobId", jobId,
                "mediaId", media.getId(),
                "readUrl", storage.signedGetQuarantine(
                        media.getQuarantinePath(), Duration.ofMinutes(5)),
                "previewWriteUrl", storage.signedPut(
                        media.getQuarantinePath() + ".preview",
                        Duration.ofMinutes(5),
                        10 * 1024 * 1024),
                "previewQuarantinePath", media.getQuarantinePath() + ".preview",
                "claimedMimeType", media.getClaimedMimeType(),
                "callbackUrl", apiBaseUrl + "/api/v1/internal/scanner/callback");
        byte[] json = json(body);
        String digest = sha256(json);
        client.post()
                .uri("/scan")
                .header("X-Wambe-Timestamp", timestamp)
                .header("X-Wambe-Nonce", nonce.toString())
                .header("X-Wambe-Signature", hmac(timestamp + "\n" + nonce + "\n" + digest))
                .header("Content-Type", "application/json")
                .body(json)
                .retrieve()
                .toBodilessEntity();
    }

    private byte[] json(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}

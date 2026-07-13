package com.wambe.api.integration.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "wambe.storage.type", havingValue = "supabase")
public class SupabaseObjectStorage implements ObjectStoragePort {

    private final RestClient client;
    private final String storageBaseUrl;
    private final String quarantineBucket;
    private final String activeBucket;

    public SupabaseObjectStorage(
            RestClient.Builder builder,
            @Value("${wambe.supabase.url}") String supabaseUrl,
            @Value("${wambe.supabase.service-role-key}") String serviceRoleKey,
            @Value("${wambe.storage.quarantine-bucket}") String quarantineBucket,
            @Value("${wambe.storage.active-bucket}") String activeBucket) {
        this.storageBaseUrl = supabaseUrl.replaceAll("/+$", "") + "/storage/v1";
        this.client = builder
                .baseUrl(storageBaseUrl)
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                .build();
        this.quarantineBucket = quarantineBucket;
        this.activeBucket = activeBucket;
    }

    @Override
    public URI signedPut(String path, Duration ttl, long maxBytes) {
        SignedUrl response = client.post()
                .uri("/object/upload/sign/{bucket}/{path}", quarantineBucket, path)
                .header("x-upsert", "false")
                .body(Map.of("expiresIn", ttl.toSeconds()))
                .retrieve()
                .body(SignedUrl.class);
        return absolute(response == null ? null : response.uploadUrl());
    }

    @Override
    public boolean exists(String path, long expectedSize) {
        return client.head()
                .uri("/object/{bucket}/{path}", quarantineBucket, path)
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        return false;
                    }
                    long size = response.getHeaders().getContentLength();
                    return size == expectedSize;
                });
    }

    @Override
    public void promote(String sourcePath, String targetPath) {
        if (existsIn(activeBucket, targetPath)) {
            return;
        }
        client.post()
                .uri("/object/move")
                .body(Map.of(
                        "bucketId", quarantineBucket,
                        "sourceKey", sourcePath,
                        "destinationBucket", activeBucket,
                        "destinationKey", targetPath))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void delete(String path) {
        deleteFrom(quarantineBucket, path);
        deleteFrom(activeBucket, path);
    }

    @Override
    public URI signedGetQuarantine(String path, Duration ttl) {
        return signedGetFrom(quarantineBucket, path, ttl);
    }

    @Override
    public URI signedGetActive(String path, Duration ttl) {
        return signedGetFrom(activeBucket, path, ttl);
    }

    private URI signedGetFrom(String bucket, String path, Duration ttl) {
        SignedUrl response = client.post()
                .uri("/object/sign/{bucket}/{path}", bucket, path)
                .body(Map.of("expiresIn", ttl.toSeconds()))
                .retrieve()
                .body(SignedUrl.class);
        return absolute(response == null ? null : response.downloadUrl());
    }

    private void deleteFrom(String bucket, String path) {
        client.delete()
                .uri("/object/{bucket}/{path}", bucket, path)
                .exchange((request, response) -> null);
    }

    private boolean existsIn(String bucket, String path) {
        return client.head()
                .uri("/object/{bucket}/{path}", bucket, path)
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful());
    }

    private URI absolute(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("Supabase did not return a signed URL");
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return URI.create(path);
        }
        return URI.create(storageBaseUrl + (path.startsWith("/") ? path : "/" + path));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SignedUrl(
            String url,
            @JsonProperty("signedURL") String signedUrl) {

        String uploadUrl() {
            return url != null ? url : signedUrl;
        }

        String downloadUrl() {
            return signedUrl != null ? signedUrl : url;
        }
    }
}

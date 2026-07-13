package com.wambe.api.integration.storage;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wambe.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorage implements ObjectStoragePort {

    private final Path root;
    private final String apiBaseUrl;

    public LocalObjectStorage(
            @Value("${wambe.storage.local-root}") Path root,
            @Value("${wambe.api-base-url}") String apiBaseUrl) {
        this.root = root.toAbsolutePath().normalize();
        this.apiBaseUrl = apiBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public URI signedPut(String path, Duration ttl, long maxBytes) {
        return URI.create(apiBaseUrl + "/dev-storage/" + path);
    }

    @Override
    public boolean exists(String path, long expectedSize) {
        Path object = resolve("quarantine", path);
        try {
            return Files.isRegularFile(object) && Files.size(object) == expectedSize;
        } catch (IOException exception) {
            return false;
        }
    }

    @Override
    public void promote(String sourcePath, String targetPath) {
        Path source = resolve("quarantine", sourcePath);
        Path target = resolve("active", targetPath);
        try {
            if (!Files.exists(source) && Files.isRegularFile(target)) {
                return;
            }
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not promote storage object", exception);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(resolveAny(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete storage object", exception);
        }
    }

    @Override
    public URI signedGetQuarantine(String path, Duration ttl) {
        return URI.create(apiBaseUrl + "/dev-storage/" + path);
    }

    @Override
    public URI signedGetActive(String path, Duration ttl) {
        return URI.create(apiBaseUrl + "/dev-storage/" + path);
    }

    public Path quarantinePath(String path) {
        return resolve("quarantine", path);
    }

    private Path resolve(String bucket, String path) {
        Path resolved = root.resolve(bucket).resolve(path).normalize();
        if (!resolved.startsWith(root.resolve(bucket).normalize())) {
            throw new IllegalArgumentException("Invalid storage path");
        }
        return resolved;
    }

    private Path resolveAny(String path) {
        Path quarantine = resolve("quarantine", path);
        if (Files.exists(quarantine)) {
            return quarantine;
        }
        return resolve("active", path);
    }
}

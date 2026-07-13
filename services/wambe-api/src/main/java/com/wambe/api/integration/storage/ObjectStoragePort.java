package com.wambe.api.integration.storage;

import java.net.URI;
import java.time.Duration;

public interface ObjectStoragePort {

    URI signedPut(String path, Duration ttl, long maxBytes);

    boolean exists(String path, long expectedSize);

    void promote(String sourcePath, String targetPath);

    void delete(String path);

    URI signedGetQuarantine(String path, Duration ttl);

    URI signedGetActive(String path, Duration ttl);
}

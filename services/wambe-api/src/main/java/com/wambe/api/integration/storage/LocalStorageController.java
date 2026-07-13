package com.wambe.api.integration.storage;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dev-storage")
@Profile({"local", "test"})
@ConditionalOnProperty(name = "wambe.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageController {

    private static final int MAX_BYTES = 10 * 1024 * 1024;

    private final LocalObjectStorage storage;

    public LocalStorageController(LocalObjectStorage storage) {
        this.storage = storage;
    }

    @PutMapping("/**")
    ResponseEntity<Void> upload(HttpServletRequest request) throws IOException {
        String path = objectPath(request);
        byte[] content = StreamUtils.copyToByteArray(request.getInputStream());
        if (content.length == 0 || content.length > MAX_BYTES) {
            return ResponseEntity.unprocessableEntity().build();
        }
        var target = storage.quarantinePath(path);
        Files.createDirectories(target.getParent());
        Files.write(target, content);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/**")
    ResponseEntity<byte[]> download(HttpServletRequest request) throws IOException {
        var object = storage.quarantinePath(objectPath(request));
        if (!Files.isRegularFile(object)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(Files.readAllBytes(object));
    }

    private String objectPath(HttpServletRequest request) {
        return request.getRequestURI().substring("/dev-storage/".length());
    }
}

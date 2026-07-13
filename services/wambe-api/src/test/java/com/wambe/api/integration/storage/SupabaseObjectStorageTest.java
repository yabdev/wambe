package com.wambe.api.integration.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SupabaseObjectStorageTest {

    @Test
    void signsScannerReadsFromQuarantineBucket() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            exchange.getRequestBody().readAllBytes();
            byte[] response = "{\"signedURL\":\"/signed/read\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            var storage = new SupabaseObjectStorage(
                    RestClient.builder(),
                    baseUrl,
                    "service-key",
                    "media-quarantine",
                    "media-active");

            storage.signedGetQuarantine(
                    "owner/event/media/original", Duration.ofMinutes(5));

            assertThat(requestedPath.get())
                    .isEqualTo(
                            "/storage/v1/object/sign/media-quarantine/owner/event/media/original");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsUploadWhenSupabaseDoesNotReportObjectSize() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            var storage = new SupabaseObjectStorage(
                    RestClient.builder(),
                    baseUrl,
                    "service-key",
                    "media-quarantine",
                    "media-active");

            assertThat(storage.exists("owner/event/media/original", 42)).isFalse();
        } finally {
            server.stop(0);
        }
    }
}

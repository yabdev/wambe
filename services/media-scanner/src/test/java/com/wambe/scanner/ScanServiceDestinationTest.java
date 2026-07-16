package com.wambe.scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class ScanServiceDestinationTest {

    private HttpServer server;
    private String origin;
    private AtomicInteger callbackRequests;

    @BeforeEach
    void setUp() throws Exception {
        callbackRequests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/dev-storage/file", exchange -> {
            exchange.getResponseHeaders().set("Location", "/dev-storage/redirected");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/api/v1/internal/scanner/callback", exchange -> {
            callbackRequests.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        origin = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void doesNotFollowStorageRedirectOrContactCallback() {
        ClamAvClient clamAv = mock(ClamAvClient.class);
        var policy = new ScannerDestinationPolicy(
                "",
                origin,
                true,
                new ScannerMetrics(new SimpleMeterRegistry()));
        var service = new ScanService(
                clamAv,
                JsonMapper.builder().build(),
                RestClient.builder(),
                policy,
                "scanner-test-secret",
                10 * 1024 * 1024);
        var request = new ScanRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                URI.create(origin + "/dev-storage/file"),
                URI.create(origin + "/dev-storage/preview"),
                "owner/event/media/preview",
                URI.create(origin + "/api/v1/internal/scanner/callback"),
                "image/png");

        assertThatThrownBy(() -> service.scan(request))
                .isInstanceOf(RuntimeException.class);

        assertThat(callbackRequests.get()).isZero();
        verifyNoInteractions(clamAv);
    }
}

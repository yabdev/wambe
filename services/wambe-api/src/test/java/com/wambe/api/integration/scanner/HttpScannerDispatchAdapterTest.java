package com.wambe.api.integration.scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.wambe.api.integration.storage.ObjectStoragePort;
import com.wambe.api.media.persistence.MediaEntity;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpScannerDispatchAdapterTest {

    private HttpServer server;
    private String scannerUrl;
    private AtomicReference<String> identityHeader;
    private AtomicReference<byte[]> receivedBody;

    @BeforeEach
    void setUp() throws Exception {
        identityHeader = new AtomicReference<>();
        receivedBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/scan", exchange -> {
            identityHeader.set(exchange.getRequestHeaders().getFirst("X-Serverless-Authorization"));
            receivedBody.set(exchange.getRequestBody().readAllBytes());
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
        scannerUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsGoogleIdentityAndHmacHeadersWithBoundedJson() {
        var adapter = adapter(() -> Optional.of("google-id-token"));

        adapter.dispatch(UUID.randomUUID(), media());

        assertThat(identityHeader.get()).isEqualTo("Bearer google-id-token");
        assertThat(receivedBody.get())
                .isNotEmpty()
                .hasSizeLessThanOrEqualTo(16_384);
    }

    @Test
    void localDispatchOmitsServerlessIdentityHeader() {
        var adapter = adapter(Optional::<String>empty);

        adapter.dispatch(UUID.randomUUID(), media());

        assertThat(identityHeader.get()).isNull();
    }

    private HttpScannerDispatchAdapter adapter(ScannerIdentityTokenProvider identity) {
        ObjectStoragePort storage = mock(ObjectStoragePort.class);
        when(storage.signedGetQuarantine(any(), any()))
                .thenReturn(URI.create(
                        "https://project.supabase.co/storage/v1/object/sign/media-quarantine/object?token=read"));
        when(storage.signedPut(any(), any(), anyLong()))
                .thenReturn(URI.create(
                        "https://project.supabase.co/storage/v1/object/sign/media-quarantine/preview?token=write"));
        return new HttpScannerDispatchAdapter(
                RestClient.builder(),
                storage,
                new ObjectMapper(),
                identity,
                scannerUrl,
                "scanner-test-secret",
                "https://api.staging.wambe.example");
    }

    private MediaEntity media() {
        return MediaEntity.quarantine(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "invitation",
                "invite.png",
                "image/png",
                1_024,
                OffsetDateTime.now(ZoneOffset.UTC));
    }
}

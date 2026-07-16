package com.wambe.scanner;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ScannerMetrics {

    private final MeterRegistry registry;

    public ScannerMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void envelopeRejected() {
        registry.counter(
                        "wambe.security.envelope.rejected",
                        "service", "media_scanner",
                        "route", "scan")
                .increment();
    }

    public void destinationRejected(String reason) {
        registry.counter(
                        "wambe.scanner.destination.rejected",
                        "reason", destinationReason(reason))
                .increment();
    }

    private String destinationReason(String reason) {
        return switch (reason) {
            case "storage_uri",
                    "storage_scheme",
                    "storage_host",
                    "storage_path",
                    "callback_uri",
                    "callback_origin",
                    "callback_path",
                    "invalid_api_origin",
                    "invalid_supabase_origin" -> reason;
            default -> "other";
        };
    }
}

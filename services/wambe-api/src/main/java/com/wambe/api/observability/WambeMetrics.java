package com.wambe.api.observability;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class WambeMetrics {

    private final MeterRegistry registry;
    private final DistributionSummary scanJobAge;
    private final AtomicLong lastRetentionSuccess = new AtomicLong();

    public WambeMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.scanJobAge = DistributionSummary.builder("wambe.scan.job.age")
                .description("Age in seconds when a scan job is leased")
                .baseUnit("seconds")
                .register(registry);
        Gauge.builder("wambe.retention.last.success", lastRetentionSuccess, AtomicLong::get)
                .description("Epoch seconds of the last successful retention run")
                .baseUnit("seconds")
                .register(registry);
    }

    public void envelopeRejected() {
        registry.counter(
                        "wambe.security.envelope.rejected",
                        "service", "api",
                        "route", "scanner_callback")
                .increment();
    }

    public void jwtRejected(String reason) {
        registry.counter(
                        "wambe.security.jwt.rejected",
                        "reason", jwtReason(reason))
                .increment();
    }

    public void scanResult(String result) {
        registry.counter(
                        "wambe.scan.result",
                        "result", "clean".equals(result) ? "clean" : "rejected")
                .increment();
    }

    public void scanJobAge(double seconds) {
        scanJobAge.record(Math.max(0, seconds));
    }

    public void retentionOutcome(String outcome) {
        if ("success".equals(outcome)) {
            lastRetentionSuccess.set(Instant.now().getEpochSecond());
        }
        registry.counter(
                        "wambe.retention.outcome",
                        "outcome", "success".equals(outcome) ? "success" : "failure")
                .increment();
    }

    private String jwtReason(String reason) {
        return switch (reason) {
            case "missing_role", "wrong_role", "invalid_subject" -> reason;
            default -> "other";
        };
    }
}

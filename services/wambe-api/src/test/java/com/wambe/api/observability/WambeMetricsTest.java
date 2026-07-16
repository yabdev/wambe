package com.wambe.api.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

class WambeMetricsTest {

    @Test
    void exportsOnlyBoundedSecurityScanAndRetentionLabels() {
        var registry = new SimpleMeterRegistry();
        var metrics = new WambeMetrics(registry);

        metrics.envelopeRejected();
        metrics.jwtRejected("unexpected-attacker-value");
        metrics.scanResult("clean");
        metrics.scanJobAge(42);
        metrics.retentionOutcome("success");

        assertThat(registry.get("wambe.security.envelope.rejected").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("wambe.security.jwt.rejected")
                        .tag("reason", "other")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("wambe.scan.result")
                        .tag("result", "clean")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get("wambe.scan.job.age").summary().max())
                .isEqualTo(42);
        assertThat(registry.get("wambe.retention.last.success").gauge().value())
                .isPositive();
    }

    @Test
    void prometheusScanAgeSeriesMatchesAlertContract() {
        var registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        var metrics = new WambeMetrics(registry);

        metrics.scanJobAge(42);

        assertThat(registry.scrape()).contains("wambe_scan_job_age_seconds_max");
    }
}

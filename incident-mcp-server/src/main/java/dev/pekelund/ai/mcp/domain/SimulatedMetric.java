package dev.pekelund.ai.mcp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single simulated metric data-point.
 *
 * <p>In production this would be backed by a time-series database (Prometheus,
 * InfluxDB, etc.). The seed data contains recent samples for typical metrics:
 * {@code cpu_percent}, {@code memory_percent}, {@code error_rate_per_minute},
 * and {@code p95_latency_ms}. Agents query these to correlate metric anomalies
 * with the timing of reported incidents.
 */
@Entity
@Table(name = "simulated_metrics",
        indexes = {
                @Index(name = "idx_metric_service_time", columnList = "service_name,timestamp"),
                @Index(name = "idx_metric_name", columnList = "metric_name")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulatedMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    /** Metric identifier, e.g. {@code cpu_percent} or {@code p95_latency_ms}. */
    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal value;

    /** Human-readable unit, e.g. {@code %}, {@code ms}, {@code req/min}. */
    @Column(length = 50)
    private String unit;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}

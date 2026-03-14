package dev.pekelund.mcp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A single simulated log entry used for incident investigation.
 *
 * <p>In a real production system this table would be replaced by a connection
 * to an actual log aggregation platform (e.g., Elasticsearch / Loki / Splunk).
 * Here we seed realistic log entries via Flyway so that the AI agents have
 * meaningful data to query during demo scenarios.
 *
 * <p>The {@code metadata} column stores arbitrary JSON — for example,
 * {@code {"requestId":"abc","userId":"42","durationMs":5000}}.
 */
@Entity
@Table(name = "simulated_logs",
        indexes = {
                @Index(name = "idx_log_service_time", columnList = "service_name,timestamp"),
                @Index(name = "idx_log_level", columnList = "level")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulatedLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    /** Log level: {@code ERROR}, {@code WARN}, {@code INFO}, {@code DEBUG}. */
    @Column(nullable = false, length = 20)
    private String level;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    /** Optional JSON metadata attached to the log event. */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}

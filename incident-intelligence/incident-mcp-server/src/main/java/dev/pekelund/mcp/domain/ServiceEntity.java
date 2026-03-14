package dev.pekelund.mcp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a service registered in the platform.
 *
 * <p>Services are the units of deployment that can fail and trigger incidents.
 * Each service has metadata that the AI uses to understand blast radius, team
 * ownership and criticality when evaluating an incident.
 */
@Entity
@Table(name = "services")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique identifier used in log lines, metrics and incident reports. */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Team responsible for this service (used for routing and post-mortems). */
    @Column(length = 100)
    private String team;

    /** Comma-separated list of services this service depends on. */
    @Column(length = 500)
    private String dependencies;

    /**
     * Whether this service is on the critical path. Critical services receive
     * higher default severity on incidents and trigger PagerDuty-style escalation.
     */
    @Builder.Default
    private boolean critical = false;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

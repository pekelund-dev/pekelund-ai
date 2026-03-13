package dev.pekelund.ai.agent.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Incident entity as seen by the agent application.
 *
 * <p>The schema is defined by the MCP server's Flyway migration V1.
 * The agent app uses {@code spring.jpa.hibernate.ddl-auto=validate} to confirm
 * that the expected columns are present — it never modifies the schema itself.
 *
 * @see dev.pekelund.ai.mcp.domain.Incident (identical schema, separate module)
 */
@Entity
@Table(name = "incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(length = 20)
    private String severity;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "OPEN";

    @Column(length = 100)
    private String category;

    @Column(length = 500)
    private String affectedServices;

    @Column(length = 100)
    private String reportedBy;

    @Column(length = 100)
    private String assignedTo;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column(columnDefinition = "TEXT")
    private String rootCause;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

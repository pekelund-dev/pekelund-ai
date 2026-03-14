package dev.pekelund.mcp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * An operational runbook entry in the knowledge base.
 *
 * <p>Runbooks capture institutional knowledge: step-by-step procedures for
 * diagnosing and resolving known failure modes. The AI agents search the
 * runbook library when diagnosing an incident and cite relevant runbooks
 * in their recommendations.
 *
 * <p>Good runbooks follow the <em>5W+H</em> pattern: What, Why, When, Where,
 * Who and How — this content lives in {@link #content}.
 */
@Entity
@Table(name = "runbooks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Runbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    /**
     * The primary service this runbook applies to.
     * May be {@code null} for platform-wide runbooks (e.g., database failover).
     */
    @Column(length = 100)
    private String serviceName;

    /**
     * The full runbook content in Markdown. Agents include snippets of this
     * text directly in their diagnosis and resolution recommendations.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Comma-separated tags for keyword matching
     * (e.g., {@code "high-cpu,memory-leak,oom-kill"}).
     */
    @Column(length = 500)
    private String tags;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

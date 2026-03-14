package dev.pekelund.agent.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Incident timeline note — mirrors the MCP server's {@code IncidentNote} entity.
 */
@Entity
@Table(name = "incident_notes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** {@code AUTO} for AI-generated notes, {@code MANUAL} for human notes. */
    @Builder.Default
    @Column(name = "note_type", nullable = false, length = 20)
    private String noteType = "MANUAL";

    @Column(length = 100)
    private String author;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

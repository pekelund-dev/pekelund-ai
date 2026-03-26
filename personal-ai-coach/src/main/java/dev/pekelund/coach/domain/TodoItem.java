package dev.pekelund.coach.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A todo item that can be created manually or automatically by agents.
 * Todos are categorized and prioritized for easy overview.
 */
@Entity
@Table(name = "todo_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TodoCategory category = TodoCategory.GENERAL;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TodoStatus status = TodoStatus.PENDING;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** The agent that automatically created this todo, if any. */
    @Column(name = "source_agent")
    private String sourceAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum TodoCategory {
        GENERAL, CALENDAR, EMAIL, FINANCE, MENU, VACATION, COACHING
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum TodoStatus {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }
}

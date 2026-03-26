package dev.pekelund.coach.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A meal entry in a weekly menu plan.
 * Each entry represents one meal for one day of the week.
 */
@Entity
@Table(name = "menu_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Year and ISO week number, e.g. "2026-W13". */
    @Column(name = "week_year", nullable = false)
    private String weekYear;

    @Column(name = "day_of_week", nullable = false)
    private String dayOfWeek;

    @Column(name = "meal_type", nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private MealType mealType = MealType.DINNER;

    @Column(name = "recipe_name", nullable = false)
    private String recipeName;

    @Column(name = "recipe_details")
    private String recipeDetails;

    @Column
    @Builder.Default
    private Integer servings = 4;

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

    public enum MealType {
        BREAKFAST, LUNCH, DINNER, SNACK
    }
}

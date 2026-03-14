package dev.pekelund.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new incident.
 *
 * <p>Uses Java 16+ records for concise, immutable DTOs — a best practice for
 * request/response objects that should never be mutated after construction.
 *
 * @param title       Short, human-readable summary of the problem (max 255 chars)
 * @param description Detailed description of the observed behaviour, including
 *                    any error messages, affected users and approximate start time
 * @param reportedBy  Username or email of the person reporting the incident
 */
public record CreateIncidentRequest(
        @NotBlank(message = "Title must not be blank")
        @Size(max = 255, message = "Title must be 255 characters or fewer")
        String title,

        @NotBlank(message = "Description must not be blank")
        String description,

        String reportedBy
) {
}

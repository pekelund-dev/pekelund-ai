package dev.pekelund.ai.mcp.repository;

import dev.pekelund.ai.mcp.domain.IncidentNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for {@link IncidentNote}.
 */
@Repository
public interface IncidentNoteRepository extends JpaRepository<IncidentNote, Long> {

    List<IncidentNote> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}

package dev.pekelund.agent.repository;

import dev.pekelund.agent.domain.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for {@link Incident} — used by the agent application.
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /** Returns all incidents ordered by creation time (newest first). */
    List<Incident> findAllByOrderByCreatedAtDesc();

    /** Returns all open/in-progress incidents for dashboard use. */
    List<Incident> findByStatusOrderByCreatedAtDesc(String status);
}

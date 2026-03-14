package dev.pekelund.mcp.repository;

import dev.pekelund.mcp.domain.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for {@link Incident} — used by the MCP server side
 * to query incident history for the {@code get_incident_history} tool.
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /**
     * Finds resolved incidents that affected a given service, ordered by most
     * recent first. Used by AI agents to learn from past incidents.
     *
     * @param serviceName  service to search for in the affected_services column
     * @param limit        maximum number of incidents to return
     */
    @Query(value = """
            SELECT * FROM incidents
            WHERE status = 'RESOLVED'
              AND (:serviceName IS NULL
                   OR LOWER(affected_services) LIKE LOWER(CONCAT('%', :serviceName, '%')))
            ORDER BY resolved_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Incident> findResolvedByService(@Param("serviceName") String serviceName,
                                         @Param("limit") int limit);

    /**
     * Finds recent incidents (any status) by category.
     */
    @Query(value = """
            SELECT * FROM incidents
            WHERE (:category IS NULL OR LOWER(category) = LOWER(:category))
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Incident> findByCategory(@Param("category") String category,
                                  @Param("limit") int limit);
}

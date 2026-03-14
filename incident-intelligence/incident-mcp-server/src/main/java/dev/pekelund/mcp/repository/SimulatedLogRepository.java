package dev.pekelund.mcp.repository;

import dev.pekelund.mcp.domain.SimulatedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for {@link SimulatedLog}.
 */
@Repository
public interface SimulatedLogRepository extends JpaRepository<SimulatedLog, Long> {

    /**
     * Searches logs for a given service with optional level and message pattern filters.
     * Any filter parameter that is {@code null} or empty is ignored (treated as wildcard).
     *
     * @param serviceName the service to search (required)
     * @param level       log level filter or {@code null} for all levels
     * @param pattern     substring to match in message or {@code null} for all messages
     * @param limit       maximum rows to return
     */
    @Query(value = """
            SELECT * FROM simulated_logs
            WHERE service_name = :serviceName
              AND (:level   IS NULL OR UPPER(level)           = UPPER(:level))
              AND (:pattern IS NULL OR LOWER(message) LIKE LOWER(CONCAT('%', :pattern, '%')))
            ORDER BY timestamp DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<SimulatedLog> searchLogs(@Param("serviceName") String serviceName,
                                  @Param("level") String level,
                                  @Param("pattern") String pattern,
                                  @Param("limit") int limit);
}

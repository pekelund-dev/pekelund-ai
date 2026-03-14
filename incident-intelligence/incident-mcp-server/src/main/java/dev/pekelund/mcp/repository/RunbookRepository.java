package dev.pekelund.mcp.repository;

import dev.pekelund.mcp.domain.Runbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for {@link Runbook}.
 *
 * <p>The custom query performs a simple case-insensitive full-text search across
 * title, description and tags. In production this would use a proper FTS index
 * (PostgreSQL {@code tsvector} / {@code tsquery}) but for the demo an
 * {@code ILIKE} scan is sufficient.
 */
@Repository
public interface RunbookRepository extends JpaRepository<Runbook, Long> {

    /**
     * Search runbooks by service name (exact match).
     */
    List<Runbook> findByServiceNameIgnoreCase(String serviceName);

    /**
     * Keyword search across title, description and tags columns.
     *
     * @param keyword the search term (must not be {@code null})
     * @param limit   maximum number of results to return
     */
    @Query(value = """
            SELECT * FROM runbooks
            WHERE LOWER(title)       LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(description) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(tags)        LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY updated_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Runbook> searchByKeyword(@Param("keyword") String keyword,
                                  @Param("limit") int limit);
}

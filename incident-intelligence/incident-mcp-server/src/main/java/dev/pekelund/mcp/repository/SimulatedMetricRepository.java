package dev.pekelund.mcp.repository;

import dev.pekelund.mcp.domain.SimulatedMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for {@link SimulatedMetric}.
 */
@Repository
public interface SimulatedMetricRepository extends JpaRepository<SimulatedMetric, Long> {

    /**
     * Retrieves the most recent metric samples for a service, optionally filtered
     * by metric name.
     *
     * @param serviceName name of the service
     * @param metricName  specific metric or {@code null} for all metrics
     * @param limit       maximum rows to return
     */
    @Query(value = """
            SELECT * FROM simulated_metrics
            WHERE service_name = :serviceName
              AND (:metricName IS NULL OR metric_name = :metricName)
            ORDER BY timestamp DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<SimulatedMetric> getLatestMetrics(@Param("serviceName") String serviceName,
                                           @Param("metricName") String metricName,
                                           @Param("limit") int limit);
}

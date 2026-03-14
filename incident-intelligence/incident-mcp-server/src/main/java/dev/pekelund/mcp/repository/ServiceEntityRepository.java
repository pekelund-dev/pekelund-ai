package dev.pekelund.mcp.repository;

import dev.pekelund.mcp.domain.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data repository for {@link ServiceEntity}.
 *
 * <p>Used by the {@code ServiceStatusTool} to look up service metadata by name,
 * as well as during the seeding process to check for existing records.
 */
@Repository
public interface ServiceEntityRepository extends JpaRepository<ServiceEntity, Long> {

    Optional<ServiceEntity> findByName(String name);
}

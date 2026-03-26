package dev.pekelund.coach.repository;

import dev.pekelund.coach.domain.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    List<UserPreference> findByUserIdAndCategory(Long userId, String category);

    Optional<UserPreference> findByUserIdAndCategoryAndPreferenceKey(
            Long userId, String category, String preferenceKey);

    List<UserPreference> findByUserId(Long userId);
}

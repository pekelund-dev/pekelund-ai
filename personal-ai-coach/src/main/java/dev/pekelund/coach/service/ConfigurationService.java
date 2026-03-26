package dev.pekelund.coach.service;

import dev.pekelund.coach.domain.UserPreference;
import dev.pekelund.coach.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages user preferences stored as key-value pairs per category.
 *
 * <p>Categories include: calendar, email, finance, menu, vacation, coaching, general.
 */
@Service
public class ConfigurationService {

    private final UserPreferenceRepository preferenceRepository;

    public ConfigurationService(UserPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * Gets all preferences for a user in a specific category.
     */
    public Map<String, String> getPreferences(Long userId, String category) {
        return preferenceRepository.findByUserIdAndCategory(userId, category)
                .stream()
                .collect(Collectors.toMap(
                        UserPreference::getPreferenceKey,
                        p -> p.getPreferenceValue() != null ? p.getPreferenceValue() : ""));
    }

    /**
     * Gets all preferences for a user across all categories.
     */
    public Map<String, Map<String, String>> getAllPreferences(Long userId) {
        List<UserPreference> prefs = preferenceRepository.findByUserId(userId);
        return prefs.stream()
                .collect(Collectors.groupingBy(
                        UserPreference::getCategory,
                        Collectors.toMap(
                                UserPreference::getPreferenceKey,
                                p -> p.getPreferenceValue() != null ? p.getPreferenceValue() : "")));
    }

    /**
     * Sets a preference value, creating or updating as needed.
     */
    public void setPreference(Long userId, String category, String key, String value) {
        preferenceRepository.findByUserIdAndCategoryAndPreferenceKey(userId, category, key)
                .ifPresentOrElse(
                        existing -> {
                            existing.setPreferenceValue(value);
                            preferenceRepository.save(existing);
                        },
                        () -> {
                            // Need to look up the User entity
                            var pref = UserPreference.builder()
                                    .category(category)
                                    .preferenceKey(key)
                                    .preferenceValue(value)
                                    .build();
                            preferenceRepository.save(pref);
                        }
                );
    }
}

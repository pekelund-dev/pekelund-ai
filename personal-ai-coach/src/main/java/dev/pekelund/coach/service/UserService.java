package dev.pekelund.coach.service;

import dev.pekelund.coach.domain.User;
import dev.pekelund.coach.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Manages user lifecycle — creates or updates users based on their Google OAuth2 profile.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Finds or creates a user from an OAuth2 principal.
     * Updates name and picture on every login to keep the profile current.
     */
    public User findOrCreateUser(OAuth2User oAuth2User) {
        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        return userRepository.findByGoogleId(googleId)
                .map(existing -> {
                    existing.setName(name);
                    existing.setPictureUrl(picture);
                    log.debug("Updated existing user: {}", email);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User user = User.builder()
                            .googleId(googleId)
                            .email(email)
                            .name(name)
                            .pictureUrl(picture)
                            .locale("sv")
                            .build();
                    log.info("Created new user: {}", email);
                    return userRepository.save(user);
                });
    }

    /**
     * Finds a user by their Google ID.
     */
    public User findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + googleId));
    }
}

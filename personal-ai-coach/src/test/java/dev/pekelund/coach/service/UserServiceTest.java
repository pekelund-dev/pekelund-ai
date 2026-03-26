package dev.pekelund.coach.service;

import dev.pekelund.coach.domain.User;
import dev.pekelund.coach.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void findOrCreateUserCreatesNewUserWhenNotFound() {
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("sub")).thenReturn("google-123");
        when(oAuth2User.getAttribute("email")).thenReturn("test@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Test User");
        when(oAuth2User.getAttribute("picture")).thenReturn("https://example.com/photo.jpg");

        User result = userService.findOrCreateUser(oAuth2User);

        assertThat(result.getGoogleId()).isEqualTo("google-123");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("Test User");
        assertThat(result.getLocale()).isEqualTo("sv");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findOrCreateUserUpdatesExistingUser() {
        User existing = User.builder()
                .id(1L)
                .googleId("google-123")
                .email("test@example.com")
                .name("Old Name")
                .build();
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("sub")).thenReturn("google-123");
        when(oAuth2User.getAttribute("email")).thenReturn("test@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("New Name");
        when(oAuth2User.getAttribute("picture")).thenReturn("https://example.com/new-photo.jpg");

        User result = userService.findOrCreateUser(oAuth2User);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getPictureUrl()).isEqualTo("https://example.com/new-photo.jpg");
        verify(userRepository).save(existing);
    }

    @Test
    void findByGoogleIdThrowsWhenNotFound() {
        when(userRepository.findByGoogleId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByGoogleId("unknown"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not found");
    }
}

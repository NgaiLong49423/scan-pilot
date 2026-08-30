package com.scanpilot.persistence;

import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.entity.UserSessionEntity;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.persistence.repository.UserSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("User and Session Persistence Tests")
class UserAndSessionPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @org.junit.jupiter.api.BeforeEach
    void cleanDatabase() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        sessionRepository.flush();
        userRepository.flush();
    }

    @Nested
    @DisplayName("User Entity CRUD Tests")
    class UserCrudTests {

        @Test
        @DisplayName("Should persist and find user by ID, githubUserId, and login")
        void shouldPersistAndFindUser() {
            UserEntity user = UserEntity.builder()
                    .githubUserId(12345678L)
                    .login("octocat")
                    .name("The Octocat")
                    .avatarUrl("https://avatars.githubusercontent.com/u/12345678")
                    .email("octocat@github.com")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            UserEntity saved = userRepository.save(user);

            assertThat(saved.getId()).isNotNull();

            Optional<UserEntity> byId = userRepository.findById(saved.getId());
            assertThat(byId).isPresent();
            assertThat(byId.get().getLogin()).isEqualTo("octocat");

            Optional<UserEntity> byGithubId = userRepository.findByGithubUserId(12345678L);
            assertThat(byGithubId).isPresent();
            assertThat(byGithubId.get().getEmail()).isEqualTo("octocat@github.com");

            Optional<UserEntity> byLogin = userRepository.findByLogin("octocat");
            assertThat(byLogin).isPresent();
            assertThat(byLogin.get().getName()).isEqualTo("The Octocat");
        }

        @Test
        @DisplayName("Should enforce uniqueness of github_user_id")
        void shouldEnforceGithubUserIdUniqueness() {
            UserEntity user1 = UserEntity.builder()
                    .githubUserId(99999L)
                    .login("user1")
                    .createdAt(Instant.now())
                    .build();
            userRepository.saveAndFlush(user1);

            UserEntity user2 = UserEntity.builder()
                    .githubUserId(99999L)
                    .login("user2")
                    .createdAt(Instant.now())
                    .build();

            assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Should update user information")
        void shouldUpdateUser() {
            UserEntity user = userRepository.save(UserEntity.builder()
                    .githubUserId(55555L)
                    .login("initial_login")
                    .name("Initial Name")
                    .createdAt(Instant.now())
                    .build());

            user.setName("Updated Name");
            user.setUpdatedAt(Instant.now());
            UserEntity updated = userRepository.saveAndFlush(user);

            UserEntity fetched = userRepository.findById(updated.getId()).orElseThrow();
            assertThat(fetched.getName()).isEqualTo("Updated Name");
            assertThat(fetched.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("User Session Entity Tests")
    class UserSessionTests {

        @Test
        @DisplayName("Should persist and retrieve sessions by session_id and user_id")
        void shouldPersistAndFindSessions() {
            UserEntity user = userRepository.save(UserEntity.builder()
                    .githubUserId(77777L)
                    .login("session_tester")
                    .createdAt(Instant.now())
                    .build());

            Instant now = Instant.now();
            UserSessionEntity session = UserSessionEntity.builder()
                    .sessionId("sess_abc123xyz456")
                    .userId(user.getId())
                    .accessToken("gho_dummy_token_12345")
                    .installationId(987654L)
                    .createdAt(now)
                    .expiresAt(now.plus(7, ChronoUnit.DAYS))
                    .build();

            sessionRepository.save(session);

            Optional<UserSessionEntity> foundSession = sessionRepository.findBySessionId("sess_abc123xyz456");
            assertThat(foundSession).isPresent();
            assertThat(foundSession.get().getUserId()).isEqualTo(user.getId());
            assertThat(foundSession.get().getInstallationId()).isEqualTo(987654L);

            List<UserSessionEntity> userSessions = sessionRepository.findByUserId(user.getId());
            assertThat(userSessions).hasSize(1);
        }

        @Test
        @DisplayName("Should delete session by sessionId and delete expired sessions")
        void shouldDeleteSessions() {
            UserEntity user = userRepository.save(UserEntity.builder()
                    .githubUserId(88888L)
                    .login("cleanup_tester")
                    .createdAt(Instant.now())
                    .build());

            Instant now = Instant.now();
            UserSessionEntity activeSession = UserSessionEntity.builder()
                    .sessionId("sess_active")
                    .userId(user.getId())
                    .createdAt(now)
                    .expiresAt(now.plus(1, ChronoUnit.DAYS))
                    .build();

            UserSessionEntity expiredSession = UserSessionEntity.builder()
                    .sessionId("sess_expired")
                    .userId(user.getId())
                    .createdAt(now.minus(2, ChronoUnit.DAYS))
                    .expiresAt(now.minus(1, ChronoUnit.DAYS))
                    .build();

            sessionRepository.saveAllAndFlush(List.of(activeSession, expiredSession));

            // Clean up expired sessions
            sessionRepository.deleteByExpiresAtBefore(now);
            sessionRepository.flush();

            assertThat(sessionRepository.findBySessionId("sess_expired")).isEmpty();
            assertThat(sessionRepository.findBySessionId("sess_active")).isPresent();

            // Delete specific session
            sessionRepository.deleteBySessionId("sess_active");
            sessionRepository.flush();
            assertThat(sessionRepository.findBySessionId("sess_active")).isEmpty();
        }
    }
}

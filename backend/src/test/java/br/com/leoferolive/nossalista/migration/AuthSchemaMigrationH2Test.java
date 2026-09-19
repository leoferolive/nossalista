package br.com.leoferolive.nossalista.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral V19 checks against the default H2 test database.
 *
 * <p>The migration metadata is checked against PostgreSQL in
 * {@link UserTableMigrationTest}; these tests exercise the constraints and
 * rolling-compatibility rows against H2 as well.</p>
 */
@SpringBootTest
class AuthSchemaMigrationH2Test {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldEnforceIdentityForeignKeysAndUniqueKeys() {
        UUID firstUserId = insertUser();
        UUID secondUserId = insertUser();
        try {
            insertIdentity(firstUserId, "GOOGLE", "https://accounts.google.com", "subject-1");

            assertThatThrownBy(() -> insertIdentity(
                secondUserId, "GOOGLE", "https://accounts.google.com", "subject-1"))
                .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> insertIdentity(
                firstUserId, "GOOGLE", "https://accounts.google.com", "subject-2"))
                .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> insertIdentity(
                UUID.randomUUID(), "GOOGLE", "https://accounts.google.com", "subject-3"))
                .isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", firstUserId, secondUserId);
        }
    }

    @Test
    void shouldEnforceOAuthCodeHashUniquenessAndUserForeignKey() {
        UUID userId = insertUser();
        try {
            insertHashCode(UUID.randomUUID(), "duplicate-hash", userId);

            assertThatThrownBy(() -> insertHashCode(UUID.randomUUID(), "duplicate-hash", userId))
                .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> insertHashCode(
                UUID.randomUUID(), "orphan-hash", UUID.randomUUID()))
                .isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void shouldDefaultSessionVersionAndAllowLegacyAndHashRows() {
        UUID userId = insertUser();
        UUID legacyCodeId = UUID.randomUUID();
        UUID newCodeId = UUID.randomUUID();
        try {
            Integer sessionVersion = jdbcTemplate.queryForObject(
                "SELECT session_version FROM users WHERE id = ?", Integer.class, userId);
            assertThat(sessionVersion).isZero();

            jdbcTemplate.update("""
                INSERT INTO oauth_authorization_codes
                    (id, code, jwt, expires_at)
                VALUES (?, ?, ?, ?)
                """, legacyCodeId, "legacy-code", "legacy-jwt", expirationTime());
            jdbcTemplate.update("""
                INSERT INTO oauth_authorization_codes
                    (id, code_hash, user_id, expires_at)
                VALUES (?, ?, ?, ?)
                """, newCodeId, "hash-only-code", userId, expirationTime());

            Integer rows = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM oauth_authorization_codes
                WHERE id IN (?, ?)
                """, Integer.class, legacyCodeId, newCodeId);
            assertThat(rows).isEqualTo(2);
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void shouldRejectDuplicateEmailValues() {
        UUID firstUserId = insertUser();
        try {
            assertThatThrownBy(() -> insertUserWithEmail(firstUserId, "duplicate@example.com"))
                .isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", firstUserId);
        }
    }

    private UUID insertUser() {
        UUID userId = UUID.randomUUID();
        insertUserWithEmail(userId, userId + "@example.com");
        return userId;
    }

    private UUID insertUserWithEmail(UUID userId, String email) {
        jdbcTemplate.update("""
            INSERT INTO users (id, username, email, password, name, auth_provider, role, email_verified)
            VALUES (?, ?, ?, ?, ?, 'EMAIL', 'USER', TRUE)
            """, userId, "user-" + userId, email, "password", "Test User");
        return userId;
    }

    private void insertIdentity(UUID userId, String provider, String issuer, String subject) {
        jdbcTemplate.update("""
            INSERT INTO user_auth_identities
                (id, provider, issuer, subject, user_id, provider_email, provider_email_verified)
            VALUES (?, ?, ?, ?, ?, ?, TRUE)
            """, UUID.randomUUID(), provider, issuer, subject, userId, "provider@example.com");
    }

    private void insertHashCode(UUID id, String hash, UUID userId) {
        jdbcTemplate.update("""
            INSERT INTO oauth_authorization_codes
                (id, code_hash, user_id, expires_at)
            VALUES (?, ?, ?, ?)
            """, id, hash, userId, expirationTime());
    }

    private LocalDateTime expirationTime() {
        return LocalDateTime.now().plusHours(1);
    }
}

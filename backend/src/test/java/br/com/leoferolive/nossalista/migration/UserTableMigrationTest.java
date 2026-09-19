package br.com.leoferolive.nossalista.migration;

import br.com.leoferolive.nossalista.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integração para migration V1__create_users_table.sql.
 *
 * <p>Estende {@link AbstractPostgresIT}: valida o schema criado pelo Flyway
 * contra PostgreSQL real (Testcontainers), não contra o dialeto de
 * compatibilidade do H2 — ver T1 da Onda 2 (honestidade de métrica). O
 * Postgres também expõe {@code INFORMATION_SCHEMA.COLUMNS} (padrão SQL),
 * então as mesmas queries funcionam sem alteração.</p>
 */
@SpringBootTest
class UserTableMigrationTest extends AbstractPostgresIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateUsersTableWithCorrectColumns() {
        // Verify table was created by Flyway migration
        String query = """
            SELECT LOWER(COLUMN_NAME)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(TABLE_NAME) = 'users'
            AND LOWER(TABLE_SCHEMA) = 'public'
            ORDER BY ORDINAL_POSITION
            """;

        List<String> columns = jdbcTemplate.queryForList(query, String.class);

        // Verify all expected columns exist (order may vary in H2)
        // email_verified adicionado em V10 (Q2.7 — verificação de e-mail).
        assertThat(columns)
            .containsExactlyInAnyOrder("id", "username", "email", "password", "name",
                           "avatar_url", "auth_provider", "role", "created_at", "updated_at",
                           "onboarding_completed_at", "email_verified", "session_version");
    }

    @Test
    void shouldCreateExternalIdentityTable() {
        List<String> columns = jdbcTemplate.queryForList("""
            SELECT LOWER(COLUMN_NAME)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(TABLE_NAME) = 'user_auth_identities'
            AND LOWER(TABLE_SCHEMA) = 'public'
            ORDER BY ORDINAL_POSITION
            """, String.class);

        assertThat(columns).containsExactlyInAnyOrder(
            "id", "provider", "issuer", "subject", "user_id", "provider_email",
            "provider_email_verified", "created_at", "updated_at");
    }

    @Test
    void shouldAddOAuthHandoffColumnsWithoutRemovingLegacyColumns() {
        List<String> columns = jdbcTemplate.queryForList("""
            SELECT LOWER(COLUMN_NAME)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(TABLE_NAME) = 'oauth_authorization_codes'
            AND LOWER(TABLE_SCHEMA) = 'public'
            ORDER BY ORDINAL_POSITION
            """, String.class);

        assertThat(columns).containsExactlyInAnyOrder(
            "id", "code", "jwt", "expires_at", "created_at", "code_hash", "user_id",
            "consumed_at");
    }

    @Test
    void shouldDefaultSessionVersionToZeroAndAllowLegacyOAuthRows() {
        UUID userId = insertUser();
        UUID legacyCodeId = UUID.randomUUID();
        UUID newCodeId = UUID.randomUUID();
        try {
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

        Map<String, Object> sessionVersion = jdbcTemplate.queryForMap("""
            SELECT IS_NULLABLE, COLUMN_DEFAULT
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(TABLE_NAME) = 'users'
            AND LOWER(TABLE_SCHEMA) = 'public'
            AND LOWER(COLUMN_NAME) = 'session_version'
            """);

        assertThat(sessionVersion.get("IS_NULLABLE")).isEqualTo("NO");
        assertThat(String.valueOf(sessionVersion.get("COLUMN_DEFAULT"))).contains("0");

        List<Map<String, Object>> handoffColumns = jdbcTemplate.queryForList("""
            SELECT LOWER(COLUMN_NAME) AS column_name, IS_NULLABLE
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(TABLE_NAME) = 'oauth_authorization_codes'
            AND LOWER(TABLE_SCHEMA) = 'public'
            AND LOWER(COLUMN_NAME) IN ('code_hash', 'user_id', 'consumed_at')
            """);

        assertThat(handoffColumns).hasSize(3)
            .allSatisfy(column -> assertThat(column.get("IS_NULLABLE")).isEqualTo("YES"));
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
        UUID firstCodeId = UUID.randomUUID();
        try {
            insertHashCode(firstCodeId, "duplicate-hash", userId);

            assertThatThrownBy(() -> insertHashCode(UUID.randomUUID(), "duplicate-hash", userId))
                .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> insertHashCode(
                UUID.randomUUID(), "orphan-hash", UUID.randomUUID()))
                .isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
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

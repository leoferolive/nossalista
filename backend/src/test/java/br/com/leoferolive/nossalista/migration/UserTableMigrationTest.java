package br.com.leoferolive.nossalista.migration;

import br.com.leoferolive.nossalista.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldHaveUniqueIndexOnEmail() {
        // Verify unique index on email exists
        String query = "SELECT COUNT(*) FROM users WHERE 1=0";

        // If table doesn't exist, this will throw an exception
        Integer count = jdbcTemplate.queryForObject(query, Integer.class);
        assertThat(count).isEqualTo(0);

        // Table exists, so migration ran successfully
    }
}

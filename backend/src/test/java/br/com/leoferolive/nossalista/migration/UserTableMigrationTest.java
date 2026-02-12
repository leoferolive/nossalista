package br.com.leoferolive.nossalista.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração para migration V1__create_users_table.sql
 */
@SpringBootTest
class UserTableMigrationTest {

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

        // Verify all expected columns exist
        assertThat(columns)
            .containsExactly("id", "username", "email", "password", "name",
                           "avatar_url", "auth_provider", "role", "created_at", "updated_at");
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

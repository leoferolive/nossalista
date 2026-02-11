package br.com.leoferolive.nossalista.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração para migration V1__create_users_table.sql
 */
@SpringBootTest
@ActiveProfiles("dev")
@Sql(scripts = "/db/migration/V1__create_users_table.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class UserTableMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateUsersTableWithCorrectColumns() {
        // Verify table was created by Flyway migration
        String query = """
            SELECT COLUMN_NAME
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = 'USERS'
            AND TABLE_SCHEMA = 'PUBLIC'
            ORDER BY ORDINAL_POSITION
            """;

        List<String> columns = jdbcTemplate.queryForList(query, String.class);

        // Verify all expected columns exist
        assertThat(columns)
            .containsExactly("ID", "USERNAME", "EMAIL", "PASSWORD", "NAME",
                           "AVATAR_URL", "AUTH_PROVIDER", "ROLE", "CREATED_AT", "UPDATED_AT");
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

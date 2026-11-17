package com.ttt.vivm.identity;

import com.ttt.vivm.support.MySqlIntegrationSupport;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigrationIT {

    private static final String RUN_ID = UUID.randomUUID().toString().replace("-", "");
    private static final String EMPTY_DATABASE = "vivm_r2_empty_it_" + RUN_ID;
    private static final String LEGACY_DATABASE = "vivm_r2_legacy_it_" + RUN_ID;
    private static final MySqlIntegrationSupport.DatabaseConfig CONFIG = MySqlIntegrationSupport.loadConfig();

    @AfterAll
    static void removeDatabases() {
        CONFIG.dropDatabase(EMPTY_DATABASE);
        CONFIG.dropDatabase(LEGACY_DATABASE);
    }

    @Test
    void emptyDatabaseMigratesToVersionThreeAndJdbcTransactionRollsBackAcrossTables() {
        CONFIG.createDatabase(EMPTY_DATABASE);
        String url = CONFIG.databaseUrl(EMPTY_DATABASE);

        migrate(url).migrate();

        assertThat(schemaVersion(url)).isEqualTo("3");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, CONFIG.username(), CONFIG.password());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO `user` "
                            + "(user_name, user_pwd, role, oss_space, email, balance, account_status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    "rollback-user", "encoded", 0, 0, "rollback@example.com", 0, "ACTIVE");
            jdbc.update("INSERT INTO operation_task "
                            + "(id, actor_id, operation_type, resource_type, status, step, version, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    "rollback-operation", 1, "TEST", "USER", "PENDING", "CREATED", 0, LocalDateTime.now());
            throw new IllegalStateException("force rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM operation_task", Long.class)).isZero();
    }

    @Test
    void legacyDatabaseIsBaselinedExplicitlyThenRelationsAreBackfilled() throws SQLException {
        CONFIG.createDatabase(LEGACY_DATABASE);
        String url = CONFIG.databaseUrl(LEGACY_DATABASE);
        Flyway versionOne = Flyway.configure()
                .dataSource(url, CONFIG.username(), CONFIG.password())
                .target("1")
                .load();
        versionOne.migrate();

        try (Connection connection = DriverManager.getConnection(url, CONFIG.username(), CONFIG.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history");
            statement.executeUpdate("INSERT INTO `user` "
                    + "(user_id, user_name, user_pwd, role, oss_space, email, balance) "
                    + "VALUES (1, 'legacy-user', 'encoded', 0, 0, 'legacy@example.com', 0)");
            statement.executeUpdate("INSERT INTO pool (id, name, dir) VALUES (1, 'pool-a', '/images/pool-a')");
            statement.executeUpdate("INSERT INTO img "
                    + "(img_id, img_name, img_type, is_delete, status) VALUES "
                    + "(1, 'base-a', 0, 0, 0), (2, 'delta-a', 1, 0, 0)");
            statement.executeUpdate("INSERT INTO img_pool (img_id, pool_id) VALUES (1, 1), (2, 1)");
            statement.executeUpdate("INSERT INTO img_child (img_child_id, img_id) VALUES (2, 1)");
            statement.executeUpdate("INSERT INTO vm "
                    + "(vm_id, vm_name, vm_type, vm_status) VALUES (1, 'vm-a', 1, 0)");
            statement.executeUpdate("INSERT INTO img_vm (img_id, vm_id) VALUES (2, 1)");
            statement.executeUpdate("INSERT INTO version (id, name) VALUES (1, 'version-a')");
            statement.executeUpdate("INSERT INTO version_pool (pool_id, version_id) VALUES (1, 1)");
        }

        Flyway flyway = migrate(url);
        flyway.baseline();
        flyway.migrate();

        try (Connection connection = DriverManager.getConnection(url, CONFIG.username(), CONFIG.password());
             Statement statement = connection.createStatement()) {
            assertThat(singleLong(statement, "SELECT pool_id FROM img WHERE img_id = 2")).isEqualTo(1);
            assertThat(singleLong(statement, "SELECT parent_image_id FROM img WHERE img_id = 2")).isEqualTo(1);
            assertThat(singleString(statement, "SELECT storage_kind FROM img WHERE img_id = 2")).isEqualTo("DELTA");
            assertThat(singleString(statement, "SELECT visibility FROM img WHERE img_id = 2")).isEqualTo("PRIVATE");
            assertThat(singleLong(statement, "SELECT image_id FROM vm WHERE vm_id = 1")).isEqualTo(2);
            assertThat(singleLong(statement, "SELECT pool_id FROM version WHERE id = 1")).isEqualTo(1);
            assertThat(singleLong(statement, "SELECT root_image_id FROM pool WHERE id = 1")).isEqualTo(1);
            assertThat(singleString(statement, "SELECT account_status FROM `user` WHERE user_id = 1"))
                    .isEqualTo("ACTIVE");
        }
    }

    private static Flyway migrate(String url) {
        return Flyway.configure()
                .dataSource(url, CONFIG.username(), CONFIG.password())
                .baselineVersion("1")
                .load();
    }

    private static String schemaVersion(String url) {
        try (Connection connection = DriverManager.getConnection(url, CONFIG.username(), CONFIG.password());
             Statement statement = connection.createStatement()) {
            return singleString(statement,
                    "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1");
        } catch (SQLException error) {
            throw new IllegalStateException("Unable to read Flyway schema version", error);
        }
    }

    private static long singleLong(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static String singleString(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }
}

package com.ttt.vivm.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class MySqlIntegrationSupport {

    private MySqlIntegrationSupport() {
    }

    public static DatabaseConfig loadConfig() {
        String configPath = System.getProperty("vivm.it.config");
        if (configPath == null) {
            throw new IllegalStateException("Set -Dvivm.it.config to a local datasource properties file");
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of(configPath))) {
            properties.load(input);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read integration-test datasource properties", error);
        }
        return new DatabaseConfig(
                properties.getProperty("spring.datasource.url"),
                properties.getProperty("spring.datasource.username"),
                properties.getProperty("spring.datasource.password"));
    }

    public record DatabaseConfig(String sourceUrl, String username, String password) {

        public String databaseUrl(String database) {
            int pathStart = sourceUrl.indexOf('/', "jdbc:mysql://".length());
            int queryStart = sourceUrl.indexOf('?');
            String suffix = queryStart >= 0 ? sourceUrl.substring(queryStart) : "";
            return sourceUrl.substring(0, pathStart + 1) + database + suffix;
        }

        String serverUrl() {
            int pathStart = sourceUrl.indexOf('/', "jdbc:mysql://".length());
            int queryStart = sourceUrl.indexOf('?');
            String suffix = queryStart >= 0 ? sourceUrl.substring(queryStart) : "";
            return sourceUrl.substring(0, pathStart + 1) + suffix;
        }

        public void createDatabase(String database) {
            executeOnServer("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4");
        }

        public void dropDatabase(String database) {
            executeOnServer("DROP DATABASE IF EXISTS `" + database + "`");
        }

        private void executeOnServer(String sql) {
            try (Connection connection = DriverManager.getConnection(serverUrl(), username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException error) {
                throw new IllegalStateException("Unable to prepare MySQL integration-test database", error);
            }
        }
    }
}

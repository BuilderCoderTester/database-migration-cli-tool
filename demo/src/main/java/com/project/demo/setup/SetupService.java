package com.project.demo.setup;

import com.project.demo.config.ConfigManager;
import com.project.demo.config.DatabaseConfig;
import com.project.demo.utility.RandomGenerator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class SetupService {

    public boolean testConnection(DatabaseConfig config) {

        try (Connection connection = DriverManager.getConnection(
                config.getJdbcUrl(),
                config.getUsername(),
                config.getPassword())) {

            return true;

        } catch (Exception e) {

            e.printStackTrace();
            return false;

        }

    }

    public void createRole(DatabaseConfig config,
                           String roleName,
                           String rolePassword) throws SQLException {

        String sql = String.format("""
                        DO
                        $$
                        BEGIN
                            IF NOT EXISTS (
                                SELECT
                                FROM pg_roles
                                WHERE rolname = '%s'
                            ) THEN
                        
                                CREATE ROLE %s
                                LOGIN
                                PASSWORD '%s';
                        
                            END IF;
                        END
                        $$;
                        """,
                roleName,
                roleName,
                rolePassword);

        try (Connection connection = DriverManager.getConnection(
                config.getJdbcUrl(),
                config.getUsername(),
                config.getPassword());

             Statement statement = connection.createStatement()) {

            statement.execute(sql);

        }

    }

    public void createDatabase(DatabaseConfig config,
                               String databaseName,
                               String owner) throws SQLException {

        try (Connection connection = DriverManager.getConnection(
                config.getJdbcUrl(),
                config.getUsername(),
                config.getPassword());

             Statement statement = connection.createStatement()) {

            var rs = statement.executeQuery("""
                SELECT 1
                FROM pg_database
                WHERE datname='%s'
                """.formatted(databaseName));

            if (!rs.next()) {

                statement.execute("""
                    CREATE DATABASE %s
                    OWNER %s
                    """.formatted(databaseName, owner));

            }

        }

    }



    public boolean initialize(DatabaseConfig adminConfig) {

        try {

            String appUser = adminConfig.getUsername();

            String appPassword = adminConfig.getPassword();

            String database = adminConfig.getDatabase();

            createRole(adminConfig, appUser, appPassword);

            createDatabase(adminConfig, database, appUser);

            DatabaseConfig application = new DatabaseConfig();

            application.setHost(adminConfig.getHost());
            application.setPort(adminConfig.getPort());
            application.setDatabase(database);
            application.setUsername(appUser);
            application.setPassword(appPassword);

            new ConfigManager().save(application);

            return true;

        } catch (Exception e) {

            e.printStackTrace();
            return false;

        }

    }
}
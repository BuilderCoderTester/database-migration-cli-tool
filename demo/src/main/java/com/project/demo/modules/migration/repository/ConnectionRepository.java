package com.project.demo.modules.migration.repository;

import com.project.demo.component.ConnectionContext;
import com.project.demo.infrastructure.sqlQueries.MigrationQuery;
import com.project.demo.modules.migration.dto.connection.request.ConnectionRequest;
import com.project.demo.modules.migration.dto.connection.response.ConnectionResponseDto;
import com.project.demo.modules.migration.model.ConnectionConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
@Getter
@Slf4j
public class ConnectionRepository {
    @Autowired
    private ConnectionRepo connectionRepo;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ConnectionContext connectionContext;

    public void createSystemTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute(MigrationQuery.CREATE_TABLE_SUB_CONNECTION);
        stmt.execute(MigrationQuery.CREATE_TABLE_SUM_MIGRATION);
        stmt.execute(MigrationQuery.CREATE_TABLE_MIGRATION_LOCK);
    }

    public ConnectionResponseDto connect(ConnectionRequest request) {

        String dbName = request.getDatabase();

        if (!dbName.matches("[a-zA-Z0-9_]+")) {
            return new ConnectionResponseDto(false, "Invalid database name", null);
        }

        try {

            String adminUrl = buildJdbcUrl(
                    request.getHost(),
                    request.getPort(),
                    "postgres"
            );

            createDatabaseIfNotExists(adminUrl, request, dbName);

            String targetUrl = buildJdbcUrl(
                    request.getHost(),
                    request.getPort(),
                    dbName
            );

            testConnection(targetUrl, request);

            ConnectionConfig saved = connectionRepo.save(
                    new ConnectionConfig()
                            .setName(request.getName())
                            .setHost(request.getHost())
                            .setPort(request.getPort())
                            .setDatabase(dbName)
                            .setUsername(request.getUsername())
                            .setPassword(request.getPassword())
                            .setSchema(request.getSchema())
                            .setUrl(targetUrl)
            );

            return new ConnectionResponseDto(
                    true,
                    "Connection successful",
                    saved.getConnectionId()
            );

        } catch (SQLException e) {
            log.error("Failed to connect to database {}", dbName, e);
            return new ConnectionResponseDto(false, e.getMessage(), null);
        }
    }

    private void createDatabaseIfNotExists(
            String adminUrl,
            ConnectionRequest request,
            String database) throws SQLException {

        DataSource dataSource = buildDataSource(adminUrl, request);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            connection.setAutoCommit(true);

            statement.executeUpdate("CREATE DATABASE \"" + database + "\"");

            log.info("Database created: {}", database);

        } catch (SQLException ex) {

            // duplicate_database
            if ("42P04".equals(ex.getSQLState())) {
                log.info("Database already exists: {}", database);
                return;
            }

            throw ex;
        }
    }

    private void testConnection(
            String url,
            ConnectionRequest request) throws SQLException {

        DataSource dataSource = buildDataSource(url, request);

        try (Connection ignored = dataSource.getConnection()) {
            log.info("Connection verified.");
        }
    }

    private DataSource buildDataSource(
            String url,
            ConnectionRequest request) {

        return DataSourceBuilder.create()
                .url(url)
                .username(request.getUsername())
                .password(request.getPassword())
                .build();
    }

    private String buildJdbcUrl(
            String host,
            int port,
            String database) {

        return String.format(
                "jdbc:postgresql://%s:%d/%s",
                host,
                port,
                database
        );
    }

    public Connection activeConnection(String databaseName) throws SQLException {
        log.debug("Opening active connection for database {}", databaseName);
        String sql = """
                    SELECT connection_id FROM connections WHERE database = ?
                """;
        String url = """
                SELECT url from connections where connection_id = ?
                """;
        Long connection_id = jdbcTemplate.queryForObject(sql, Long.class, databaseName);
        connectionContext.setCurrentConnectionId(connection_id);
        connectionContext.setCurrentDatabase(databaseName);
        String dbUrl = jdbcTemplate.queryForObject(url, String.class, connection_id);
        log.debug("Resolved JDBC URL for connection {}", connection_id);

        assert dbUrl != null;
        return DriverManager.getConnection(
                dbUrl,
                "postgres",
                "sigilotech"
        );
    }

    public List<ConnectionConfig> getAllConnections() throws SQLException {

        List<ConnectionConfig> databases = new ArrayList<>();
        String sql = """
                SELECT datname
                FROM pg_database
                WHERE datistemplate = false
                ORDER BY datname
                """;
        Connection metadataConn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/postgres",
                "postgres",
                "sigilotech"
        );
        try (
                PreparedStatement ps = metadataConn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                System.out.println("Coming...");

                ConnectionConfig config = new ConnectionConfig();

                String database = rs.getString("datname");
                System.out.println("Database: " + database);

                config.setDatabase(database);

                config.setHost("localhost");
                config.setPort(5432);

                System.out.println("Host: " + config.getHost());
                System.out.println("Port: " + config.getPort());

                String url = buildJdbcUrl(
                        config.getHost(),
                        config.getPort(),
                        config.getDatabase());

                System.out.println("Generated URL: " + url);

                config.setUrl(url);

                System.out.println("Config URL: " + config.getUrl());

                databases.add(config);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return databases;
    }

    public void saveConnection(ConnectionRequest connectionRequest) {

    }
}

package com.project.demo.service;

import com.project.demo.component.ConnectionContext;
import com.project.demo.dto.ConnectionInfoResponse;
import com.project.demo.dto.ConnectionRequest;
import com.project.demo.dto.ConnectionResponse;
import com.project.demo.model.ConnectionConfig;
import com.project.demo.repository.ConnectionRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ConnectionService {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ConnectionContext connectionContext;
    @Autowired
    private ConnectionRepo connectionRepo;


    // this send the metadata
    public ConnectionInfoResponse getActiveConnectionInfo(Long connectionId) {
        ConnectionConfig conn = connectionRepo.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("No active metadata"));
        return new ConnectionInfoResponse(
                conn.getHost(),
                conn.getDatabase(),
                conn.getPort(),
                "migration" // can also make dynamic later
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

        Connection newConnection = DriverManager.getConnection(
                dbUrl,
                "postgres",
                "sigilotech"
        );
        return newConnection;
    }
    public List<ConnectionConfig> getAllConnections() {

        List<ConnectionConfig> databases = new ArrayList<>();

        String sql = """
            SELECT datname
            FROM pg_database
            WHERE datistemplate = false
            ORDER BY datname
            """;

        try (Connection conn = activeConnection("postgres");
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                ConnectionConfig config = new ConnectionConfig();

                config.setDatabase(rs.getString("datname"));

                config.setHost("localhost"); // or current host
                config.setPort(5432);        // or current port

                databases.add(config);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return databases;
    }


    public ConnectionResponse connect(ConnectionRequest request) {

        String adminUrl = "jdbc:postgresql://"
                + request.getHost() + ":"
                + request.getPort() + "/postgres";

        String dbName = request.getDatabase();

        if (!dbName.matches("[a-zA-Z0-9_]+")) {
            return new ConnectionResponse(false, "Invalid database name", null);
        }

        try {
            // 🔹 1. Connect to postgres DB
            DataSource adminDs = DataSourceBuilder.create()
                    .url(adminUrl)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .build();

            // 🔹 2. Create DB (optional)

            try (Connection conn = adminDs.getConnection();
                 Statement stmt = conn.createStatement()) {

                conn.setAutoCommit(true);

                String sql = "CREATE DATABASE \"" + dbName + "\"";
                stmt.executeUpdate(sql);

                log.info("Database created: {}", dbName);

            } catch (SQLException e) {
                // 42P04 = duplicate_database
                if (!"42P04".equals(e.getSQLState())) {
                    throw e;
                }
                log.info("Database already exists: {}", dbName);
            }

            // 🔹 3. Test connection to target DB
            String targetUrl = "jdbc:postgresql://"
                    + request.getHost() + ":"
                    + request.getPort() + "/"
                    + dbName;

            DataSource targetDs = DataSourceBuilder.create()
                    .url(targetUrl)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .build();

            try (Connection ignored = targetDs.getConnection()) {
                // success
            }

            // 🔹 4. Save config
            ConnectionConfig config = new ConnectionConfig();
            config.setName(request.getName());
            config.setHost(request.getHost());
            config.setPort(request.getPort());
            config.setDatabase(dbName);
            config.setUsername(request.getUsername());
            config.setPassword(request.getPassword());
            config.setSchema(request.getSchema());
            config.setUrl(targetUrl);

            ConnectionConfig saved = connectionRepo.save(config);

            return new ConnectionResponse(true, "Connection successful", saved.getConnectionId());

        } catch (SQLException e) {
            return new ConnectionResponse(false, e.getMessage(), null);
        }
    }
}

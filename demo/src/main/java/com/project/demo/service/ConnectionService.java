package com.project.demo.service;

import com.project.demo.component.ConnectionContext;
import com.project.demo.dto.ConnectionInfoResponse;
import com.project.demo.dto.ConnectionRequest;
import com.project.demo.dto.ConnectionResponse;
import com.project.demo.model.ConnectionConfig;
import com.project.demo.repository.ConnectionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;

@Service
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
        System.out.println(databaseName);
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
        System.out.println("conneciton url " + dbUrl);

        Connection newConnection = DriverManager.getConnection(
                dbUrl,
                "postgres",
                "sigilotech"
        );
        return newConnection;
    }

    // this send the actual connection
//    public Connection getConnection(Long connectionId) {
//        System.out.println("the conneciton id at conenciton service:"+connectionId);
//        PreparedStatement pst = conn.prepareStatement("SELECT current_database()");
//        ConnectionConfig config = connectionRepository.findById(connectionId)
//                .orElseThrow(() -> new RuntimeException("No active connection"));
//        System.out.println(config);
//        try {
//            String url = "jdbc:mysql://" + config.getHost() + ":" + config.getPort()
//                    + "/" + config.getDatabase();
//
//            Connection connection =  DriverManager.getConnection(
//                    url,
//                    config.getUsername(),
//                    config.getPassword()
//            );
//            if (connection == null || !connection.isValid(5)) {
//                throw new RuntimeException("Connection is not valid");
//            }
//            return connection;
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to create DB connection", e);
//        }
//    }


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

                System.out.println("Database created: " + dbName);

            } catch (SQLException e) {
                // 42P04 = duplicate_database
                if (!"42P04".equals(e.getSQLState())) {
                    throw e;
                }
                System.out.println("Database already exists: " + dbName);
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
package com.project.demo.service;

import com.project.demo.component.ConnectionContext;
import com.project.demo.dto.ConnectionInfoResponse;
import com.project.demo.model.ConnectionConfig;
import com.project.demo.repository.ConnectionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Service
public class ConnectionService {
    private final ConnectionRepo connectionRepository;
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ConnectionContext connectionContext;
    public ConnectionService(ConnectionRepo connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    // this send the metadata
    public ConnectionInfoResponse getActiveConnectionInfo(Long connectionId) {
        ConnectionConfig conn = connectionRepository.findById(connectionId)
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
}
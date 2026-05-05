package com.project.demo.service;

import com.project.demo.dto.ConnectionInfoResponse;
import com.project.demo.model.ConnectionConfig;
import com.project.demo.repository.ConnectionRepo;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;

@Service
public class ConnectionService {
    private final ConnectionRepo connectionRepository;

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
    // this send the actual connection
    public Connection getConnection(Long connectionId) {
        System.out.println("the conneciton id at conenciton service:"+connectionId);
        ConnectionConfig config = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("No active connection"));
        System.out.println(config);
        try {
            String url = "jdbc:mysql://" + config.getHost() + ":" + config.getPort()
                    + "/" + config.getDatabase();

            Connection connection =  DriverManager.getConnection(
                    url,
                    config.getUsername(),
                    config.getPassword()
            );
            if (connection == null || !connection.isValid(5)) {
                throw new RuntimeException("Connection is not valid");
            }
            return connection;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DB connection", e);
        }
    }
}
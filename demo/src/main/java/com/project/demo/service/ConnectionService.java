package com.project.demo.service;

import com.project.demo.dto.ConnectionInfoResponse;
import com.project.demo.model.ConnectionConfig;
import com.project.demo.repository.ConnectionRepo;
import org.springframework.stereotype.Service;

@Service
public class ConnectionService {
    private final ConnectionRepo connectionRepository;

    public ConnectionService(ConnectionRepo connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    public ConnectionInfoResponse getActiveConnectionInfo(Long connectionId) {
        ConnectionConfig conn = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("No active connection"));
        return new ConnectionInfoResponse(
                conn.getHost(),
                conn.getDatabase(),
                conn.getPort(),
                "migration" // can also make dynamic later
        );
    }
}
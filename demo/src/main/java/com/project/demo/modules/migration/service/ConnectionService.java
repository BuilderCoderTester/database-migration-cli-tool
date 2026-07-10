package com.project.demo.modules.migration.service;

import com.project.demo.modules.migration.dto.connection.response.ConnectionInfoResponseDto;
import com.project.demo.modules.migration.dto.connection.request.ConnectionRequest;
import com.project.demo.modules.migration.dto.connection.response.ConnectionResponseDto;
import com.project.demo.modules.migration.model.ConnectionConfig;
import com.project.demo.modules.migration.repository.ConnectionRepo;
import com.project.demo.modules.migration.repository.ConnectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ConnectionService {

    @Autowired
    private ConnectionRepo connectionRepo;
    @Autowired
    private ConnectionRepository connectionRepository;

    public void createSystemTables(Connection connection) throws SQLException {
        connectionRepository.createSystemTables(connection);
    }

    public ConnectionInfoResponseDto getActiveConnectionInfo(Long connectionId) {
        ConnectionConfig conn = connectionRepo.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("No active metadata"));
        return new ConnectionInfoResponseDto(
                conn.getHost(),
                conn.getDatabase(),
                conn.getPort(),
                "migration"
        );
    }

    public Connection activeConnection(String databaseName) throws SQLException, IOException {
        return connectionRepository.activeConnection(databaseName);
    }

    public List<ConnectionConfig> getAllConnections() throws SQLException, IOException {
        List<ConnectionConfig> allConneciton = connectionRepository.getAllConnections();

       return allConneciton;
    }

    public ConnectionResponseDto connect(ConnectionRequest request) {
        return connectionRepository.connect(request);
    }

    public void saveConnection(ConnectionRequest connectionRequest) {
         connectionRepository.saveConnection(connectionRequest);
    }
}

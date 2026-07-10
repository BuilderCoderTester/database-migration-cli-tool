package com.project.demo.api.rest.Controller;
import com.project.demo.modules.migration.dto.connection.response.ActiveResponseDto;
import com.project.demo.modules.migration.dto.connection.response.ConnectionInfoResponseDto;
import com.project.demo.modules.migration.dto.connection.request.ConnectionRequest;
import com.project.demo.modules.migration.dto.connection.response.ConnectionResponseDto;
import com.project.demo.modules.migration.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
@Slf4j
public class ConnectionController {

    @Autowired
    private  ConnectionService connectionService;

    @PostMapping("/connect")
    public ConnectionResponseDto connect(@RequestBody ConnectionRequest connection) throws SQLException {
        return connectionService.connect(connection);
    }

    @PostMapping("/set-active")
    public ActiveResponseDto setActive(@RequestBody Map<String, String> req) throws SQLException, IOException {
        String databaseName = req.get("database");
        Connection conn = connectionService.activeConnection(databaseName);
        System.out.println("yes ");
        PreparedStatement dbStmt = conn.prepareStatement("SELECT current_database()");
        ResultSet dbRs = dbStmt.executeQuery();
        if (dbRs.next()) {
            log.info("Connected to database {}", dbRs.getString(1));
        }

        connectionService.createSystemTables(conn);
        return new ActiveResponseDto(true, "Connection is established");
    }

    @GetMapping("/info")
    public ConnectionInfoResponseDto getInfo(@RequestParam("connectionId") Long connectionId) {
        return connectionService.getActiveConnectionInfo(connectionId);
    }

}

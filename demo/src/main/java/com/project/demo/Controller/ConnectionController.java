package com.project.demo.Controller;
import com.project.demo.dto.*;
import com.project.demo.service.ConnectionService;
import com.project.demo.service.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.List;
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
    public ConnectionResponse connect(@RequestBody ConnectionRequest connection) throws SQLException {
        return connectionService.connect(connection);
    }

    @PostMapping("/set-active")
    public ApiResponse setActive(@RequestBody Map<String, String> req) throws SQLException {
        String databaseName = req.get("database");
        Connection conn = connectionService.activeConnection(databaseName);

        PreparedStatement dbStmt = conn.prepareStatement("SELECT current_database()");
        ResultSet dbRs = dbStmt.executeQuery();
        if (dbRs.next()) {
            log.info("Connected to database {}", dbRs.getString(1));
        }

        createSystemTables(conn);
        printUserTables(conn);

        return new ApiResponse(true, "Connection is established");
    }

    @GetMapping("/info")
    public ConnectionInfoResponse getInfo(@RequestParam("connectionId") Long connectionId) {
        return connectionService.getActiveConnectionInfo(connectionId);
    }

    // ─── Helpers ─────────────────────────────────────────────

    private void createSystemTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        String connectionQuery = """
            CREATE TABLE IF NOT EXISTS sub_connections (
                connection_id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255),
                host VARCHAR(255),
                port INTEGER,
                database_name VARCHAR(255),
                username VARCHAR(255),
                password VARCHAR(255),
                schema VARCHAR(255),
                url TEXT
            );
            """;

        String migrationQuery = """
            CREATE TABLE IF NOT EXISTS sub_migration (
                id BIGSERIAL PRIMARY KEY,
                version VARCHAR(50) NOT NULL UNIQUE,
                description VARCHAR(255),
                script TEXT,
                checksum VARCHAR(64),
                executed_at TIMESTAMP,
                execution_time BIGINT,
                success BOOLEAN DEFAULT FALSE,
                error_message TEXT,
                error_stack_trace TEXT,
                retry_count INT DEFAULT 0,
                dirty BOOLEAN DEFAULT FALSE,
                repeatable BOOLEAN DEFAULT FALSE,
                name VARCHAR(255),
                connection_id BIGINT
            );
            """;

        String lockQuery = """
            CREATE TABLE IF NOT EXISTS migration_lock (
                connection_id BIGINT PRIMARY KEY,
                locked BOOLEAN NOT NULL DEFAULT FALSE,
                locked_at TIMESTAMP,
                locked_by VARCHAR(255),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                heartbeat_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                last_completed_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            """;

        stmt.execute(connectionQuery);
        stmt.execute(migrationQuery);
        stmt.execute(lockQuery);
    }

    private void printUserTables(Connection conn) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("""
            SELECT schemaname, tablename
            FROM pg_catalog.pg_tables
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
            ORDER BY schemaname, tablename
            """);

        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            String schema = rs.getString("schemaname");
            String table = rs.getString("tablename");
            log.debug("Discovered table {}.{}", schema, table);
        }
    }

}

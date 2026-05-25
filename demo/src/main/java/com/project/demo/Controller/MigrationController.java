package com.project.demo.Controller;

import com.project.demo.dto.*;
import com.project.demo.dto.request.MigrationRequest;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationLogs;
import com.project.demo.model.MigrationScript;
import com.project.demo.service.ConnectionService;
import com.project.demo.service.LogService;
import com.project.demo.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MigrationController {

    private final LogService logService;
    private final MigrationService migrationService;
    private final ConnectionService connectionService;

    //CREATE CONNECTION
    @PostMapping("/connect")
    public ConnectionResponse connection(
            @RequestBody ConnectionRequest connection
    ) throws SQLException {
        System.out.println(connection.getName());
        return migrationService.connect(connection);
    }

    // ACTIVATE THE DATABASE CONNECTION WITH SPECIFIC ID
    @PostMapping("/set-active")
public ApiResponse setActive(
        @RequestBody Map<String, Object> req
) {

    try {

        String databaseName =
                String.valueOf(
                        req.get("database")
                );

        System.out.println(
                "Database Name: "
                + databaseName
        );

        Connection conn =
                migrationService
                        .activeConnection(
                                databaseName
                        );

        if (conn == null) {

            return new ApiResponse(
                    false,
                    "Connection failed"
            );
        }

        Statement stmt =
                conn.createStatement();

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

                    version VARCHAR(50)
                    NOT NULL UNIQUE,

                    description VARCHAR(255),

                    script TEXT,

                    checksum VARCHAR(64),

                    executed_at TIMESTAMP,

                    execution_time BIGINT,

                    success BOOLEAN
                    DEFAULT FALSE,

                    error_message TEXT,

                    error_stack_trace TEXT,

                    retry_count INT
                    DEFAULT 0,

                    dirty BOOLEAN
                    DEFAULT FALSE,

                    repeatable BOOLEAN
                    DEFAULT FALSE,

                    name VARCHAR(255),

                    connection_id BIGINT
                );
                """;

        String migrationLock = """
                CREATE TABLE IF NOT EXISTS migration_lock (

                    connection_id BIGINT
                    PRIMARY KEY,

                    locked BOOLEAN
                    NOT NULL DEFAULT FALSE,

                    locked_at TIMESTAMP,

                    locked_by VARCHAR(255),

                    created_at TIMESTAMP
                    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                    updated_at TIMESTAMP
                    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                    heartbeat_at TIMESTAMP
                    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                    last_completed_time TIMESTAMP
                    NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                """;

        stmt.execute(connectionQuery);

        stmt.execute(migrationQuery);

        stmt.execute(migrationLock);

        conn.close();

        return new ApiResponse(
                true,
                "Connection activated successfully"
        );

    } catch (Exception e) {

        e.printStackTrace();

        return new ApiResponse(
                false,
                e.getMessage()
        );
    }
}

    // ✅ INIT THE MIGRATION DATABASE
    @PostMapping("/init")
    public ApiResponse initialize() {
        migrationService.initialize();
        return new ApiResponse(true, "Migration schema initialized");
    }

    // RETURNS THE DATABASE CONNECTION ID
    @GetMapping("/get-connection")
public ResponseEntity<?> sendConnectionId() {

    Long id = migrationService.getConnectionId();

    System.out.println("Current Connection ID: " + id);

    if (id == null) {

        return ResponseEntity
                .status(404)
                .body("No active connection");
    }

    return ResponseEntity.ok(id);
}

    // ✅ RETURNS PENDING MIGRATION COMPLETE
    @GetMapping("/pending")
    public List<MigrationScript> list(@RequestParam("connectionId") Long connectionId) throws SQLException {
        return migrationService.listAllPendingMigration(connectionId);
    }

    // ✅ RETURNS THE STATUS OF THE MIGRATION TABLE AND FILES
    @GetMapping("/status")
    public StatusResponse status(@RequestParam("connectionId") Long connectionId) throws SQLException {
        return migrationService.status(connectionId);
    }

    // ✅ MIGRATE
    @PostMapping("/migrate")
    public MigrationResult migrate(@RequestParam("connectionId") Long connectionId) throws SQLException {

        MigrationRequest migrationRequest = new MigrationRequest();
        migrationRequest.setConnectionId(connectionId);

        return migrationService.migrate(migrationRequest);
    }

    @PostMapping("/rollback-verison")
    // not yet implemented. do it later ,
    public ApiResponse rollbackByVersion(@RequestParam(required = true) String targetVersion, @RequestParam("connectionId") Long connectionId) {
        System.out.println("the connection " + connectionId);
        return new ApiResponse(true, migrationService.rollback(targetVersion, connectionId));

    }

    // ✅ ROLLBACK
    @PostMapping("/rollback")
    public ApiResponse rollback(
            @RequestParam(required = false) String targetVersion, @RequestParam("connectionId") Long connectionId
    ) {
        System.out.println("the connection " + connectionId);
        return new ApiResponse(true, migrationService.rollback(targetVersion, connectionId));
    }

    // ✅ REPAIR
    @PostMapping("/repair")
    public ApiResponse repair() {
        return new ApiResponse(true, migrationService.repair());
    }

    // ✅ HISTORY
    @GetMapping("/history")
    public List<Migration> history(@RequestParam("connectionId") Long connectionId) throws SQLException {
        return migrationService.history(connectionId);
    }

    // ✅ CREATE
    @PostMapping("/create")
    public ApiResponse create(
            @RequestParam String version,
            @RequestParam String description,
            @RequestParam(required = false) String migrateUp,
            @RequestParam(required = false) String migrateDown
    ) {
        return new ApiResponse(true,
                migrationService.create(version, description, migrateUp, migrateDown));
    }

    // ✅ VALIDATE
    @PostMapping("/validate")
    public ApiResponse validate(@RequestParam("connectionId") Long connectionId) {
        return new ApiResponse(true, migrationService.validate(connectionId));
    }

    //ACTIVITY LOGS - SHOW ALL ACTIVITIES EXECUTED.
    @GetMapping("/logs")
    public List<MigrationLogs> getAllActivites() {
        return logService.getAllActivities();
    }

    @GetMapping("/info")
    public ConnectionInfoResponse getInfo(@RequestParam("connectionId") Long connectionId) {
        System.out.println(connectionId);
        ConnectionInfoResponse info = connectionService.getActiveConnectionInfo(connectionId);
        System.out.println(info.getDatabase());
        System.out.println(info.getHost());
        System.out.println(info.getPort());
        return info;
    }
    @GetMapping("/tables")
public ResponseEntity<?> getTables() {

    try {

        List<String> tables = migrationService.getTables();

        return ResponseEntity.ok(tables);

    } catch (Exception e) {

        return ResponseEntity
                .status(500)
                .body(e.getMessage());
    }
}
@GetMapping("/table-data/{tableName}")
public ResponseEntity<?> getTableData(
        @PathVariable String tableName
) {

    try {

        List<Map<String, Object>> data =
                migrationService
                        .getTableData(tableName);

        return ResponseEntity.ok(data);

    } catch (Exception e) {

        e.printStackTrace();

        return ResponseEntity
                .status(500)
                .body(e.getMessage());
    }
}
}
//package com.project.demo.Controller;
//
//import com.project.demo.dto.*;
//import com.project.demo.dto.request.MigrationRequest;
//import com.project.demo.model.Migration;
//import com.project.demo.model.MigrationLogs;
//import com.project.demo.model.MigrationScript;
//import com.project.demo.service.ConnectionService;
//import com.project.demo.service.LogService;
//import com.project.demo.service.MigrationService;
//import com.project.demo.service.MigrationTableService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//import java.sql.*;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/migrations")
//@RequiredArgsConstructor
//@CrossOrigin("*")
//public class MigrationController {
//
//    private final LogService logService;
//    private final MigrationService migrationService;
//    private final ConnectionService connectionService;
//    private final MigrationTableService migrationTableService;
//    //CREATE CONNECTION
//    @PostMapping("/connect")
//    public ConnectionResponse connection(
//            @RequestBody ConnectionRequest connection
//    ) throws SQLException {
//        System.out.println(connection.getName());
//        return migrationService.connect(connection);
//    }
//
//    // ACTIVATE THE DATABASE CONNECTION WITH SPECIFIC ID
//    @PostMapping("/set-active")
//    public ApiResponse setActive(@RequestBody Map<String, String> req) throws SQLException {
//        String databaseName = req.get("database");
//        Connection conn = migrationService.activeConnection(databaseName);
//
//        PreparedStatement pst = conn.prepareStatement("SELECT current_database()");
//        PreparedStatement pst_1 = conn.prepareStatement("""
//                    SELECT schemaname, tablename
//                    FROM pg_catalog.pg_tables
//                    WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
//                    ORDER BY schemaname, tablename
//                """);
//        ResultSet rs = pst.executeQuery();
//
//        String connection_querry = """
//                CREATE TABLE IF NOT EXISTS sub_connections (
//                    connection_id BIGSERIAL PRIMARY KEY,
//
//                    name VARCHAR(255),
//                    host VARCHAR(255),
//                    port INTEGER,
//                    database_name VARCHAR(255),
//                    username VARCHAR(255),
//                    password VARCHAR(255),
//                    schema VARCHAR(255),
//                    url TEXT
//                );
//                """;
//        String migration_querry = """
//
//                    CREATE TABLE IF NOT EXISTS sub_migration (
//                     id BIGSERIAL PRIMARY KEY,
//
//                     version VARCHAR(50) NOT NULL UNIQUE,
//                     description VARCHAR(255),
//
//                     script TEXT,
//                     checksum VARCHAR(64),
//
//                     executed_at TIMESTAMP,
//                     execution_time BIGINT,
//
//                     success BOOLEAN DEFAULT FALSE,
//
//                     error_message TEXT,
//                     error_stack_trace TEXT,
//
//                     retry_count INT DEFAULT 0,
//
//                     dirty BOOLEAN DEFAULT FALSE,
//                     repeatable BOOLEAN DEFAULT FALSE,
//
//                     name VARCHAR(255),
//                     connection_id BIGINT  -- ✅ correct type
//
//                 );
//                """;
//        String migration_lock = """
//                CREATE TABLE IF NOT EXISTS migration_lock (
//
//                    connection_id BIGINT PRIMARY KEY,
//
//                    locked BOOLEAN NOT NULL DEFAULT FALSE,
//
//                    locked_at TIMESTAMP,
//
//                    locked_by VARCHAR(255),
//
//                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
//
//                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
//
//                    heartbeat_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
//
//                    last_completed_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
//                );
//                """;
//
//        if (rs.next()) {
//            System.out.println("🔥 Connected to: " + rs.getString(1));
//        }
//        Statement stmt = conn.createStatement();
//
//        stmt.execute(connection_querry);
//        stmt.execute(migration_querry);
//        stmt.execute(migration_lock);
//        ResultSet rst_1 = pst_1.executeQuery();
//        while (rst_1.next()) {
//            String schema = rst_1.getString("schemaname");
//            String table = rst_1.getString("tablename");
//
//            System.out.println(schema + " → " + table);
//        }
//
//        return new ApiResponse(true, "Connection is established");
//    }
//
//    // ✅ INIT THE MIGRATION DATABASE
//    @PostMapping("/init")
//    public ApiResponse initialize() {
//        migrationService.initialize();
//        return new ApiResponse(true, "Migration schema initialized");
//    }
//
//    // RETURNS THE DATABASE CONNECTION ID
//    @GetMapping("/get-connection")
//    public Long sendConnectionId() {
//        Long id = migrationService.getConnectionId();
//        System.out.println(id);
//        return id;
//    }
//
//    // ✅ RETURNS PENDING MIGRATION COMPLETE
//    @GetMapping("/pending")
//    public List<MigrationScript> list(@RequestParam("connectionId") Long connectionId) throws SQLException {
//        return migrationService.listAllPendingMigration(connectionId);
//    }
//
//    // ✅ RETURNS THE STATUS OF THE MIGRATION TABLE AND FILES
//    @GetMapping("/status")
//    public StatusResponse status(@RequestParam("connectionId") Long connectionId) throws SQLException {
//        return migrationService.status(connectionId);
//    }
//
//    // ✅ MIGRATE
//    @PostMapping("/migrate")
//    public MigrationResult migrate(@RequestParam("connectionId") Long connectionId) throws SQLException {
//        System.out.println("come byt");
//        MigrationRequest migrationRequest = new MigrationRequest();
//        migrationRequest.setConnectionId(connectionId);
//
//        return migrationService.migrate(migrationRequest);
//    }
//
//    @PostMapping("/rollback-verison")
//    // not yet implemented. do it later ,
//    public ApiResponse rollbackByVersion(@RequestParam(required = true) String targetVersion, @RequestParam("connectionId") Long connectionId) {
//        System.out.println("the connection " + connectionId);
//        return new ApiResponse(true, migrationService.rollback(targetVersion, connectionId));
//
//    }
//
//    // ✅ ROLLBACK
//    @PostMapping("/rollback")
//    public ApiResponse rollback(
//            @RequestParam(required = false) String targetVersion, @RequestParam("connectionId") Long connectionId
//    ) {
//        System.out.println("the connection " + connectionId);
//        return new ApiResponse(true, migrationService.rollback(targetVersion, connectionId));
//    }
//
//    // ✅ REPAIR
//    @PostMapping("/repair")
//    public ApiResponse repair(@RequestParam("connectionId") Long connectionId , String versionId) throws SQLException, IOException {
//        return new ApiResponse(true, migrationService.repair(connectionId,versionId));
//    }
//
//    // ✅ HISTORY
//    @GetMapping("/history")
//    public List<Migration> history(@RequestParam("connectionId") Long connectionId) throws SQLException {
//        return migrationService.history(connectionId);
//    }
//
//    // ✅ CREATE
//    @PostMapping("/create")
//    public ApiResponse create(
//            @RequestParam String version,
//            @RequestParam String description,
//            @RequestParam(required = false) String migrateUp,
//            @RequestParam(required = false) String migrateDown
//    ) {
//        return new ApiResponse(true,
//                migrationService.create(version, description, migrateUp, migrateDown));
//    }
//
//    // ✅ VALIDATE
//    @PostMapping("/validate")
//    public ApiResponse validate(@RequestParam("connectionId") Long connectionId) {
//        return new ApiResponse(true, migrationService.validate(connectionId));
//    }
//
//    //ACTIVITY LOGS - SHOW ALL ACTIVITIES EXECUTED.
//    @GetMapping("/logs")
//    public List<MigrationLogs> getAllActivites() {
//        return logService.getAllActivities();
//    }
//
//    @GetMapping("/info")
//    public ConnectionInfoResponse getInfo(@RequestParam("connectionId") Long connectionId) {
//        System.out.println(connectionId);
//        ConnectionInfoResponse info = connectionService.getActiveConnectionInfo(connectionId);
//        System.out.println(info.getDatabase());
//        System.out.println(info.getHost());
//        System.out.println(info.getPort());
//        return info;
//    }
//
//    @GetMapping("/tables")
//    public List<String> getTables(@RequestParam("connectionId") Long connectionId) throws SQLException {
//       return migrationService.getTables(connectionId);
//    }
//
//    @GetMapping("/table/{tableName}")
//    public TableInfoDTO getTableInfo(
//            @RequestParam Long connectionId,
//            @PathVariable String tableName
//    ) throws SQLException {
//        return migrationService.getTableInfo(
//                connectionId,
//                tableName
//        );
//    }
//}
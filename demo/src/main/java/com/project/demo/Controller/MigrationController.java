package com.project.demo.Controller;

import com.project.demo.dto.*;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MigrationController {

    private final MigrationService migrationService;

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
    public ApiResponse setActive(@RequestBody Map<String, String> req){
        String databaseName = req.get("database");
        System.out.println("the database name " + databaseName);
        migrationService.activeConnection(databaseName);
        return new ApiResponse(true,"Connection is established");
    }

    // ✅ INIT THE MIGRATION DATABASE
    @PostMapping("/init")
    public ApiResponse initialize() {
        migrationService.initialize();
        return new ApiResponse(true, "Migration schema initialized");
    }

    // RETURNS THE DATABASE CONNECTION ID
    @GetMapping("/get-connection")
    public Long sendConnectionId(){
        Long id = migrationService.getConnectionId();
        System.out.println(id);
        return id;
    }

    // ✅ RETURNS PENDING MIGRATION COMPLETE
    @GetMapping("/pending")
    public List<MigrationScript> list(@RequestParam Long connectionId) {
        return migrationService.listAllPendingMigration(connectionId);
    }

    // ✅ RETURNS THE STATUS OF THE MIGRATION TABLE AND FILES
    @GetMapping("/status")
    public StatusResponse status(@RequestParam Long connectionId) {
        return migrationService.status(connectionId);
    }

    // ✅ MIGRATE
    @PostMapping("/migrate")
    public MigrationResult migrate(
            @RequestParam(required = false) String targetVersion,
            @RequestParam Long connectionId
    ) {
        return migrationService.migrate(targetVersion,connectionId);
    }

    // ✅ ROLLBACK
    @PostMapping("/rollback")
    public ApiResponse rollback(
            @RequestParam(required = false) String targetVersion , @RequestParam Long connectionId
    ) {
        System.out.println("the connection " + connectionId);
        return new ApiResponse(true, migrationService.rollback(targetVersion,connectionId));
    }

    // ✅ REPAIR
    @PostMapping("/repair")
    public ApiResponse repair() {
        return new ApiResponse(true, migrationService.repair());
    }

    // ✅ HISTORY
    @GetMapping("/history")
    public List<Migration> history(@RequestParam Long connectionId) {
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
    public ApiResponse validate(Long connectionId) {
        return new ApiResponse(true, migrationService.validate(connectionId));
    }
}
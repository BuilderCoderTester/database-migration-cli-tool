package com.project.demo.Controller;

import com.project.demo.dto.ApiResponse;
import com.project.demo.dto.MigrationResult;
import com.project.demo.dto.StatusResponse;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MigrationController {

    private final MigrationService migrationService;

    // ✅ INIT
    @PostMapping("/init")
    public ApiResponse initialize() {
        migrationService.initialize();
        return new ApiResponse(true, "Migration schema initialized");
    }

    // ✅ LIST
    @GetMapping("/pending")
    public List<MigrationScript> list() {
        return migrationService.listAllPendingMigration();
    }

    // ✅ STATUS
    @GetMapping("/status")
    public StatusResponse status() {
        return migrationService.status();
    }

    // ✅ MIGRATE
    @PostMapping("/migrate")
    public MigrationResult migrate(
            @RequestParam(required = false) String targetVersion
    ) {
        return migrationService.migrate(targetVersion);
    }

    // ✅ ROLLBACK
    @PostMapping("/rollback")
    public ApiResponse rollback(
            @RequestParam(required = false) String targetVersion
    ) {
        return new ApiResponse(true, migrationService.rollback(targetVersion));
    }

    // ✅ REPAIR
    @PostMapping("/repair")
    public ApiResponse repair() {
        return new ApiResponse(true, migrationService.repair());
    }

    // ✅ HISTORY
    @GetMapping("/history")
    public List<Migration> history() {
        return migrationService.history();
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
    public ApiResponse validate() {
        return new ApiResponse(true, migrationService.validate());
    }
}
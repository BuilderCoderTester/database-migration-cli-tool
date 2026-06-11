package com.project.demo.Controller;

import com.project.demo.dto.ApiResponse;
import com.project.demo.dto.StatusResponse;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.service.MigrationLifecycleService;
import com.project.demo.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MigrationQueryController {

    private final MigrationService migrationService;
    @Autowired
    private MigrationLifecycleService migrationLifecycleService;

    @PostMapping("/init")
    public ApiResponse initialize() {
        migrationService.initialize();
        return new ApiResponse(true, "Migration schema initialized");
    }

    @GetMapping("/pending")
    public List<MigrationScript> listPending(@RequestParam("connectionId") Long connectionId) throws SQLException {
        return migrationService.listAllPendingMigration(connectionId);
    }

    @GetMapping("/status")
    public StatusResponse status(@RequestParam("connectionId") Long connectionId) throws SQLException {
        return migrationService.status(connectionId);
    }

    @GetMapping("/history")
    public List<Migration> history(@RequestParam("connectionId") Long connectionId) throws SQLException {
        return migrationLifecycleService.history(connectionId);
    }

    @PostMapping("/validate")
    public ApiResponse validate(
            @RequestParam Long connectionId,
            @RequestParam String versionId) {

        return new ApiResponse(
                true,
                migrationLifecycleService.validate(connectionId, versionId)
        );
    }

    @GetMapping("/get-connection")
    public Long sendConnectionId() {
        return migrationService.getConnectionId();
    }

}

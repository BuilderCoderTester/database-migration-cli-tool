package com.project.demo.api.rest.Controller;

import com.project.demo.modules.migration.dto.connection.response.ActiveResponseDto;
import com.project.demo.modules.migration.dto.StatusResponse;
import com.project.demo.modules.migration.dto.response.MigrationDescriptionResponse;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.modules.migration.service.MigrationLifecycleService;
import com.project.demo.modules.migration.service.MigrationService;
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
    public ActiveResponseDto initialize() {
        migrationService.initialize();
        return new ActiveResponseDto(true, "Migration schema initialized");
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
    public List<MigrationDescriptionResponse> history(@RequestParam("connectionId") Long connectionId) throws SQLException {
        return migrationLifecycleService.history(connectionId);
    }

    @PostMapping("/validate")
    public ActiveResponseDto validate(
            @RequestParam Long connectionId,
            @RequestParam String versionId) {

        return new ActiveResponseDto(
                true,
                migrationLifecycleService.validate(connectionId, versionId)
        );
    }

    @GetMapping("/get-connection")
    public Long sendConnectionId() {
        return migrationService.getConnectionId();
    }

}

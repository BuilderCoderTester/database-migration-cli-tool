package com.project.demo.Controller;
import com.project.demo.dto.ApiResponse;
import com.project.demo.dto.StatusResponse;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MigrationQueryController {
    private final MigrationService migrationService;

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
        return migrationService.history(connectionId);
    }

    //No longer needed here
//    @PostMapping("/validate")
//    public ApiResponse validate(@RequestParam("connectionId") Long connectionId) {
//        return new ApiResponse(true, migrationService.validate(connectionId));
//    }

    @GetMapping("/get-connection")
    public Long sendConnectionId() {
        return migrationService.getConnectionId();
    }
}

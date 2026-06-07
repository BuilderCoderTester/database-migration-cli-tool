package com.project.demo.Controller;

import com.project.demo.dto.ApiResponse;
import com.project.demo.model.MigrationScript;
import com.project.demo.service.MigrationScriptService;
import com.project.demo.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MigrationScriptController {
    private final MigrationService migrationService;
    private final MigrationScriptService migrationScriptService;

    @PostMapping("/create")
    public ApiResponse create(
            @RequestParam String version,
            @RequestParam String description,
            @RequestParam(required = false) String migrateUp,
            @RequestParam(required = false) String migrateDown) {
        return new ApiResponse(true,
                migrationService.create(version, description, migrateUp, migrateDown));
    }

    @DeleteMapping("/delete")
    public ApiResponse delete(
            @RequestParam long connectionId,
            @RequestParam String versionId) throws SQLException {
        migrationService.delete(connectionId, versionId);
        return new ApiResponse(true, "Migration " + versionId + " deleted successfully");
    }

    @PostMapping("/script/update")
    public ApiResponse updateScript(
            @RequestParam String version,
            @RequestParam(required = false) String upSql,
            @RequestParam(required = false) String downSql,
            @RequestParam("connectionId") long connectionId
    ) throws IOException {
        migrationScriptService.update(version, connectionId);
        return new ApiResponse(true, "Migration " + version + " updated successfully");
    }

    @GetMapping("/script/{version}")
    public MigrationScript getScript(
            @PathVariable String version,
            @RequestParam long connectionId
    ) throws IOException {

        return migrationScriptService.viewScript(version, connectionId);
    }
}

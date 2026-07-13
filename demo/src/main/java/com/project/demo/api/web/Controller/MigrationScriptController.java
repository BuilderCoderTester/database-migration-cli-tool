package com.project.demo.api.web.Controller;

import com.project.demo.modules.migration.dto.connection.response.ActiveResponseDto;
import com.project.demo.modules.migration.dto.MigrationDetailsResponse;
import com.project.demo.modules.migration.dto.response.MigrationScriptCreateResponse;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.modules.migration.service.MigrationScriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
@Slf4j
public class MigrationScriptController {
    @Autowired
    private MigrationScriptService migrationScriptService;

    @PostMapping("/create")
    public MigrationScriptCreateResponse create(
            @RequestParam(required = false) String version,
            @RequestParam String description,
            @RequestParam(required = false) String migrateUp,
            @RequestParam(required = false) String migrateDown) {
        return (
                migrationScriptService.create(version,description, migrateUp, migrateDown));
    }

    @DeleteMapping("/delete")
    public ActiveResponseDto delete(
            @RequestParam long connectionId,
            @RequestParam String versionId) throws SQLException {
        migrationScriptService.delete(connectionId, versionId);
        return new ActiveResponseDto(true, "Migration " + versionId + " deleted successfully");
    }

    @GetMapping("/latest-version")
    public int getLatestMigrationVersion() {
        return migrationScriptService.getLatestMigrationVersion();
    }

    @PostMapping("/script/update")
    public ActiveResponseDto updateScript(
            @RequestParam(required = false) String upSql,
            @RequestParam(required = false) String downSql,
            @RequestParam("connectionId") long connectionId,
            @RequestBody String version
    ) throws IOException {
        String normalizedVersion = version.trim().replace("\"", "");
        log.info("Updating migration script version {} for connection {}", normalizedVersion, connectionId);
        migrationScriptService.update(normalizedVersion, upSql, downSql, connectionId);
        return new ActiveResponseDto(true, "Migration " + version + " updated successfully");
    }

    @GetMapping("/script/{version}")
    public MigrationScript getScript(
            @PathVariable String version,
            @RequestParam long connectionId
    ) throws IOException {

        return migrationScriptService.viewScript(version, connectionId);
    }

    @GetMapping("/details")
    public ResponseEntity<MigrationDetailsResponse> getMigrationDetails(
            @RequestParam Long connectionId,
            @RequestParam String versionId) throws IOException, SQLException {

        return ResponseEntity.ok(
                migrationScriptService.getMigrationDetails(connectionId, versionId)
        );
    }
}

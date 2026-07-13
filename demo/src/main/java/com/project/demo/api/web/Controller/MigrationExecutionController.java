package com.project.demo.api.web.Controller;

import com.project.demo.modules.migration.dto.migration.request.ExecuteMigrationRequestDto;
import com.project.demo.modules.migration.dto.migration.request.MigrationRollbackRequestDto;
import com.project.demo.modules.migration.dto.migration.request.MigrationUpdateRequestDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationRepairResponseDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationResultResponseDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationRollbackResponseDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationUpdateResponseDto;
import com.project.demo.modules.migration.service.MigrationLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MigrationExecutionController {

    @Autowired
    private MigrationLifecycleService migrationLifecycleService;

    @PostMapping("/migrate")
    public MigrationResultResponseDto executeMigrationScripts(@RequestParam("connectionId") Long connectionId) {
        return migrationLifecycleService.executeMigrationScripts(connectionId);
    }

    @PostMapping("/migrateByVersion")
    public MigrationResultResponseDto executeMigrationScriptsByVersion(
            @RequestBody ExecuteMigrationRequestDto executeMigrationRequestDto
    ) throws SQLException, IOException {
        return migrationLifecycleService.executeMigrationScriptsByVersion(executeMigrationRequestDto);
    }

    @PostMapping("/script/migrate")
    public MigrationUpdateResponseDto updateMigrationScriptByVersion(
            @RequestBody MigrationUpdateRequestDto migrationUpdateRequestDto
    ) throws IOException {
        return migrationLifecycleService.updateMigrationScriptByVersion(migrationUpdateRequestDto);
    }

    @PostMapping("/rollback")
    public MigrationRollbackResponseDto rollbackMigrationScript(
            @RequestBody MigrationRollbackRequestDto migrationRollbackRequestDto) {
        return migrationLifecycleService.rollbackMigrationScript(migrationRollbackRequestDto);
    }

    @PostMapping("/rollback-version")
    public MigrationRollbackResponseDto rollbackMigrationScriptByVersion(@RequestBody MigrationRollbackRequestDto request) {
        return migrationLifecycleService.rollbackMigrationScriptByVersion(request);
    }

    @PostMapping("/repair")
    public MigrationRepairResponseDto repairMigrationScriptsByVersion(
            @RequestParam("connectionId") Long connectionId,
            @RequestParam String versionId) throws SQLException, IOException {
        return migrationLifecycleService.repair(connectionId, versionId);
    }

}

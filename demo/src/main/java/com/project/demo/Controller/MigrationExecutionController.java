package com.project.demo.Controller;
import com.google.auto.value.AutoBuilder;
import com.project.demo.dto.ApiResponse;
import com.project.demo.dto.MigrationResult;
import com.project.demo.dto.request.MigrationRequest;
import com.project.demo.service.MigrationLifecycleService;
import com.project.demo.service.MigrationService;
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
    private  MigrationLifecycleService migrationLifecycleService;

    @PostMapping("/migrate")
    public MigrationResult migrate(@RequestParam("connectionId") Long connectionId) throws SQLException {
        MigrationRequest request = new MigrationRequest();
        request.setConnectionId(connectionId);
        System.out.println("reach point -1");
        return migrationLifecycleService.migrate(request);
    }
    @PostMapping("/script/migrate")
    public MigrationResult migrateUpdateScript(@RequestParam("connectionId") long connectionId ,@RequestParam("versionId") String version) throws SQLException, IOException {
        MigrationRequest request = new MigrationRequest();
        request.setConnectionId(connectionId);
        return migrationLifecycleService.migrateUpdatedScript(request,version);
    }
    @PostMapping("/rollback")
    public ApiResponse rollback(
            @RequestParam(required = false) String targetVersion,
            @RequestParam("connectionId") Long connectionId) {
        return new ApiResponse(true, migrationLifecycleService.rollback(targetVersion, connectionId));
    }

    @PostMapping("/rollback-version")
    public ApiResponse rollbackByVersion(
            @RequestParam(required = true) String targetVersion,
            @RequestParam("connectionId") Long connectionId) {
        return new ApiResponse(true, migrationLifecycleService.rollback(targetVersion, connectionId));
    }

    @PostMapping("/repair")
    public ApiResponse repair(
            @RequestParam("connectionId") Long connectionId,
            @RequestParam String versionId) throws SQLException, IOException {
        return new ApiResponse(true, migrationLifecycleService.repair(connectionId, versionId));
    }
}

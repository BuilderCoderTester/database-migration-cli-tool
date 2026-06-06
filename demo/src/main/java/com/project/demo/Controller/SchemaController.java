package com.project.demo.Controller;

import com.project.demo.dto.TableInfoDTO;
import com.project.demo.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SchemaController {
    private final MigrationService migrationService;

    @GetMapping("/tables")
    public List<String> getTables(@RequestParam("connectionId") Long connectionId) throws SQLException {
        return migrationService.getTables(connectionId);
    }

    @GetMapping("/table/{tableName}")
    public TableInfoDTO getTableInfo(
            @RequestParam Long connectionId,
            @PathVariable String tableName) throws SQLException {
        return migrationService.getTableInfo(connectionId, tableName);
    }
}

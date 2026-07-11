package com.project.demo.api.web.Controller;

import com.project.demo.modules.migration.dto.TableInfoDTO;
import com.project.demo.modules.migration.service.SchemaIntrospectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SchemaController {
    @Autowired
    private SchemaIntrospectionService schemaIntrospectionService;

    @GetMapping("/tables")
    public List<String> getTables(@RequestParam("connectionId") Long connectionId) throws SQLException, IOException {
        return schemaIntrospectionService.getTables(connectionId);
    }

    @GetMapping("/table/{tableName}")
    public TableInfoDTO getTableInfo(
            @RequestParam Long connectionId,
            @PathVariable String tableName) throws SQLException, IOException {
        return schemaIntrospectionService.getTableInfo(connectionId, tableName);
    }
}

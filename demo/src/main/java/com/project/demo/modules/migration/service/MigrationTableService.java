package com.project.demo.modules.migration.service;

import com.project.demo.component.ConnectionContext;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class MigrationTableService {
    ConnectionContext connectionContext;
    MigrationService migrationService;
    public List<String> getTables(Long connectionId) throws SQLException {
        Connection conn = migrationService.activeConnection( connectionContext.getCurrentDatabase());

        List<String> tables = new ArrayList<>();
        String sql = """
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'public'
        ORDER BY table_name
        """;

        try (
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {
                tables.add(rs.getString("table_name"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        log.debug("Loaded tables for connection {}: {}", connectionId, tables);
        return tables;
    }
}

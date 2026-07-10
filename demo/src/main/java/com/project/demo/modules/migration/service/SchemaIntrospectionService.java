package com.project.demo.modules.migration.service;

import com.project.demo.component.ConnectionContext;
import com.project.demo.modules.migration.dto.ColumnInfoDTO;
import com.project.demo.modules.migration.dto.TableInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@Service
@Slf4j
public class SchemaIntrospectionService {

    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ConnectionContext connectionContext;

    public List<String> getTables(Long connectionId) throws SQLException, IOException {
        Connection conn = connectionService.activeConnection(connectionContext.getCurrentDatabase());

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

    public TableInfoDTO getTableInfo(Long connectionId, String tableName) throws SQLException, IOException {

        log.debug("Loading table info for {} on connection {}", tableName, connectionId);

        Connection connection =
                connectionService.activeConnection(connectionContext.getCurrentDatabase());

        log.trace("Connection obtained for table info: {}", connection != null);

        try {

            // -------------------------------
            // Row Count
            // -------------------------------

            Long rowCount = 0L;

            String countSql = "SELECT COUNT(*) FROM " + tableName;

            try (PreparedStatement stmt = connection.prepareStatement(countSql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    rowCount = rs.getLong(1);
                }
            }

            // -------------------------------
            // Column Metadata
            // -------------------------------

            List<ColumnInfoDTO> columns = new ArrayList<>();

            String columnSql = """
                    SELECT
                        column_name,
                        data_type,
                        is_nullable
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = ?
                    ORDER BY ordinal_position
                    """;

            try (PreparedStatement stmt = connection.prepareStatement(columnSql)) {

                stmt.setString(1, tableName);

                try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {

                        columns.add(new ColumnInfoDTO(
                                rs.getString("column_name"),
                                rs.getString("data_type"),
                                "YES".equalsIgnoreCase(rs.getString("is_nullable")),
                                false
                        ));
                    }
                }
            }

            // -------------------------------
            // Table Data (First 100 Rows)
            // -------------------------------

            List<Map<String, Object>> rows = new ArrayList<>();

            String dataSql = "SELECT * FROM " + tableName + " LIMIT 100";

            try (PreparedStatement stmt = connection.prepareStatement(dataSql);
                 ResultSet rs = stmt.executeQuery()) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {

                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }

                    rows.add(row);
                }
            }

            // -------------------------------
            // DTO
            // -------------------------------

            return new TableInfoDTO(
                    tableName,
                    "public",
                    rowCount,
                    columns.size(),
                    columns,
                    rows
            );

        } catch (SQLException e) {

            log.error("Failed to load table information for {}", tableName, e);

            throw new RuntimeException(
                    "Failed to load table information for " + tableName,
                    e
            );
        }
    }

}

package com.project.demo.service;

import com.project.demo.component.ConnectionContext;
import com.project.demo.dto.ColumnInfoDTO;
import com.project.demo.dto.TableInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SchemaIntrospectionService {

    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ConnectionContext connectionContext;

    public List<String> getTables(Long connectionId) throws SQLException {
        Connection conn = connectionService.activeConnection( connectionContext.getCurrentDatabase());

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
        System.out.println(Arrays.asList(tables));
        return tables;
    }

    public TableInfoDTO getTableInfo(Long connectionId, String tableName) throws SQLException {

        System.out.println("=== getTableInfo START ===");
        System.out.println("Connection Id: " + connectionId);
        System.out.println("Table Name: " + tableName);

        Connection connection =
               connectionService.activeConnection(connectionContext.getCurrentDatabase());

        System.out.println("Connection obtained: " + (connection != null));

        try {

            // Row count
            Long rowCount = 0L;

            String countSql = "SELECT COUNT(*) FROM " + tableName;

            System.out.println("Executing Count Query: " + countSql);

            try (PreparedStatement stmt = connection.prepareStatement(countSql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    rowCount = rs.getLong(1);
                    System.out.println("Row Count: " + rowCount);
                } else {
                    System.out.println("No result returned from COUNT query");
                }
            }

            // Column information
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

            System.out.println("Executing Column Query for table: " + tableName);

            try (PreparedStatement stmt =
                         connection.prepareStatement(columnSql)) {

                stmt.setString(1, tableName);

                try (ResultSet rs = stmt.executeQuery()) {

                    int columnCounter = 0;

                    while (rs.next()) {

                        String columnName = rs.getString("column_name");
                        String dataType = rs.getString("data_type");
                        String nullable = rs.getString("is_nullable");

                        System.out.println(
                                "Column Found -> Name: " + columnName +
                                        ", Type: " + dataType +
                                        ", Nullable: " + nullable
                        );

                        columns.add(
                                new ColumnInfoDTO(
                                        columnName,
                                        dataType,
                                        "YES".equals(nullable),
                                        false
                                )
                        );

                        columnCounter++;
                    }

                    System.out.println("Total Columns Found: " + columnCounter);
                }
            }

            System.out.println("Preparing DTO...");
            System.out.println("Table Name: " + tableName);
            System.out.println("Row Count: " + rowCount);
            System.out.println("Column Count: " + columns.size());

            TableInfoDTO dto = new TableInfoDTO(
                    tableName,
                    "public",
                    rowCount,
                    columns.size(),
                    columns
            );

            System.out.println("DTO Created Successfully");
            System.out.println(dto);
            System.out.println("=== getTableInfo END ===");

            return dto;

        } catch (SQLException e) {

            System.out.println("ERROR OCCURRED");
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to load table information for " + tableName,
                    e
            );
        }
    }

}

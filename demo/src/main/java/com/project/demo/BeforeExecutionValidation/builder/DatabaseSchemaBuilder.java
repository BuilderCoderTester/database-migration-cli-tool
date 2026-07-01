package com.project.demo.BeforeExecutionValidation.builder;

import com.project.demo.BeforeExecutionValidation.model.ColumnModel;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.model.TableModel;
import org.springframework.stereotype.Component;

import java.sql.*;

@Component
public class DatabaseSchemaBuilder {

    public SchemaModel build(Connection connection) throws SQLException {

        SchemaModel schema = new SchemaModel();

        String tableSql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                AND table_type='BASE TABLE'
                ORDER BY table_name
                """;

        try (PreparedStatement tableStmt = connection.prepareStatement(tableSql);
             ResultSet tableRs = tableStmt.executeQuery()) {

            while (tableRs.next()) {

                String tableName = tableRs.getString("table_name");

                TableModel tableModel = new TableModel();
                tableModel.setTableName(tableName);

                loadColumns(connection, tableModel);

                schema.getTables().put(tableName, tableModel);
            }
        }

        return schema;
    }

    private void loadColumns(Connection connection,
                             TableModel tableModel) throws SQLException {

        String sql = """
                SELECT
                    c.column_name,
                    c.data_type,
                    c.is_nullable,
                    c.column_default,

                    EXISTS (
                        SELECT 1
                        FROM information_schema.table_constraints tc
                        JOIN information_schema.key_column_usage kcu
                          ON tc.constraint_name = kcu.constraint_name
                         AND tc.table_schema = kcu.table_schema
                        WHERE tc.constraint_type='PRIMARY KEY'
                          AND tc.table_name = c.table_name
                          AND kcu.column_name = c.column_name
                    ) AS is_primary,

                    EXISTS (
                        SELECT 1
                        FROM information_schema.table_constraints tc
                        JOIN information_schema.key_column_usage kcu
                          ON tc.constraint_name = kcu.constraint_name
                         AND tc.table_schema = kcu.table_schema
                        WHERE tc.constraint_type='FOREIGN KEY'
                          AND tc.table_name = c.table_name
                          AND kcu.column_name = c.column_name
                    ) AS is_foreign,

                    EXISTS (
                        SELECT 1
                        FROM information_schema.table_constraints tc
                        JOIN information_schema.key_column_usage kcu
                          ON tc.constraint_name = kcu.constraint_name
                         AND tc.table_schema = kcu.table_schema
                        WHERE tc.constraint_type='UNIQUE'
                          AND tc.table_name = c.table_name
                          AND kcu.column_name = c.column_name
                    ) AS is_unique

                FROM information_schema.columns c

                WHERE c.table_schema='public'
                AND c.table_name=?

                ORDER BY c.ordinal_position
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, tableModel.getTableName());

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    ColumnModel column = new ColumnModel();

                    column.setColumnName(rs.getString("column_name"));
                    column.setDataType(rs.getString("data_type"));

                    column.setNullable(
                            "YES".equalsIgnoreCase(rs.getString("is_nullable"))
                    );

                    column.setDefaultValue(
                            rs.getString("column_default")
                    );

                    column.setPrimaryKey(
                            rs.getBoolean("is_primary")
                    );

                    column.setForeignKey(
                            rs.getBoolean("is_foreign")
                    );

                    column.setUnique(
                            rs.getBoolean("is_unique")
                    );

                    if (column.isForeignKey()) {
                        loadReference(connection,
                                tableModel.getTableName(),
                                column);
                    }

                    tableModel.getColumns()
                            .put(column.getColumnName(), column);
                }
            }
        }
    }

    private void loadReference(Connection connection,
                               String tableName,
                               ColumnModel column) throws SQLException {

        String sql = """
                SELECT
                    ccu.table_name AS referenced_table,
                    ccu.column_name AS referenced_column

                FROM information_schema.table_constraints tc

                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name

                JOIN information_schema.constraint_column_usage ccu
                  ON tc.constraint_name = ccu.constraint_name

                WHERE tc.constraint_type='FOREIGN KEY'
                  AND tc.table_name=?
                  AND kcu.column_name=?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, tableName);
            ps.setString(2, column.getColumnName());

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    column.setReferencedTable(
                            rs.getString("referenced_table")
                    );

                    column.setReferencedColumn(
                            rs.getString("referenced_column")
                    );
                }
            }
        }
    }
}
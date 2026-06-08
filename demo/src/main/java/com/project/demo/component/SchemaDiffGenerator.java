package com.project.demo.component;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SchemaDiffGenerator {

    public String generateDiff(String oldSql, String newSql) throws Exception {
        System.out.println("OLD SQL:");
        System.out.println(oldSql);
        CreateTable oldTable =
                (CreateTable) CCJSqlParserUtil.parse(oldSql);

        System.out.println("NEW SQL:");
        System.out.println(newSql);
        CreateTable newTable =
                (CreateTable) CCJSqlParserUtil.parse(newSql);

        String tableName = newTable.getTable().getName();

        Map<String, ColumnDefinition> oldColumns =
                toColumnMap(oldTable);

        Map<String, ColumnDefinition> newColumns =
                toColumnMap(newTable);

        StringBuilder migration = new StringBuilder();

        // Added columns
        for (Map.Entry<String, ColumnDefinition> entry : newColumns.entrySet()) {

            String columnName = entry.getKey();

            if (!oldColumns.containsKey(columnName)) {

                ColumnDefinition column = entry.getValue();

                migration.append("ALTER TABLE ")
                        .append(tableName)
                        .append(" ADD COLUMN ")
                        .append(column.getColumnName())
                        .append(" ")
                        .append(column.getColDataType())
                        .append(";\n");
            }
        }

        // Removed columns
        for (Map.Entry<String, ColumnDefinition> entry : oldColumns.entrySet()) {

            String columnName = entry.getKey();

            if (!newColumns.containsKey(columnName)) {

                migration.append("ALTER TABLE ")
                        .append(tableName)
                        .append(" DROP COLUMN ")
                        .append(columnName)
                        .append(";\n");
            }
        }

        // Modified columns
        for (Map.Entry<String, ColumnDefinition> entry : newColumns.entrySet()) {

            String columnName = entry.getKey();

            if (oldColumns.containsKey(columnName)) {

                ColumnDefinition oldColumn =
                        oldColumns.get(columnName);

                ColumnDefinition newColumn =
                        entry.getValue();

                String oldType =
                        oldColumn.getColDataType().toString();

                String newType =
                        newColumn.getColDataType().toString();

                if (!oldType.equalsIgnoreCase(newType)) {

                    migration.append("ALTER TABLE ")
                            .append(tableName)
                            .append(" MODIFY COLUMN ")
                            .append(columnName)
                            .append(" ")
                            .append(newType)
                            .append(";\n");
                }
            }
        }

        return migration.toString();
    }

    private Map<String, ColumnDefinition> toColumnMap(
            CreateTable createTable) {

        Map<String, ColumnDefinition> map =
                new LinkedHashMap<>();

        if (createTable.getColumnDefinitions() == null) {
            return map;
        }

        for (ColumnDefinition column :
                createTable.getColumnDefinitions()) {

            map.put(
                    column.getColumnName().toLowerCase(),
                    column
            );
        }

        return map;
    }
}
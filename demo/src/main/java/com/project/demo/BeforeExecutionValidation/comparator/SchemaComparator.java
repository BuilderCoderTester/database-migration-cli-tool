package com.project.demo.BeforeExecutionValidation.comparator;

import com.project.demo.BeforeExecutionValidation.model.ColumnModel;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.model.TableModel;
import org.springframework.stereotype.Component;

@Component
public class SchemaComparator {

    public boolean isDuplicate(
            SchemaModel current,
            SchemaModel incoming) {

        for (TableModel table : incoming.getTables().values()) {

            if (isDuplicateTable(current, table)) {
                return true;
            }

        }

        return false;
    }

    private boolean isDuplicateTable(
            SchemaModel current,
            TableModel incomingTable) {
        System.out.println("Incoming: " + incomingTable.getTableName());
        System.out.println("Current tables: " + current.getTables().keySet());
        TableModel existing =
                current.getTables()
                        .get(incomingTable.getTableName());
        System.out.println("Found: " + existing);
        if (existing == null) {
            return false;
        }

        return compareTable(existing, incomingTable);

    }

    private boolean compareTable(
            TableModel existing,
            TableModel incoming) {

        // Different number of columns
        if (existing.getColumns().size() !=
                incoming.getColumns().size()) {
            return false;
        }

        for (ColumnModel incomingColumn :
                incoming.getColumns().values()) {

            ColumnModel existingColumn =
                    existing.getColumns()
                            .get(incomingColumn.getColumnName());

            if (existingColumn == null) {
                return false;
            }

            if (!compareColumn(
                    existingColumn,
                    incomingColumn)) {
                return false;
            }
        }

        // Detect removed columns
        for (String columnName :
                existing.getColumns().keySet()) {

            if (!incoming.getColumns().containsKey(columnName)) {
                return false;
            }
        }

        return true;
    }

    private boolean compareColumn(
            ColumnModel first,
            ColumnModel second) {

        if (!equalsIgnoreCase(
                first.getDataType(),
                second.getDataType())) {
            return false;
        }

        if (first.isNullable() != second.isNullable()) {
            return false;
        }

        if (first.isPrimaryKey() != second.isPrimaryKey()) {
            return false;
        }

        if (first.isForeignKey() != second.isForeignKey()) {
            return false;
        }

        if (first.isUnique() != second.isUnique()) {
            return false;
        }

        if (!equalsIgnoreCase(
                first.getReferencedTable(),
                second.getReferencedTable())) {
            return false;
        }

        if (!equalsIgnoreCase(
                first.getReferencedColumn(),
                second.getReferencedColumn())) {
            return false;
        }

        return true;
    }
    private boolean equalsIgnoreCase(
            String a,
            String b) {

        if (a == null && b == null)
            return true;

        if (a == null || b == null)
            return false;

        return a.trim()
                .equalsIgnoreCase(
                        b.trim()
                );

    }

    public boolean introducesNewChanges(
            SchemaModel current,
            SchemaModel incoming) {

        for (TableModel incomingTable : incoming.getTables().values()) {

            if (hasNewTable(current, incomingTable)) {
                return true;
            }

        }

        return false;
    }

    private boolean hasNewTable(
            SchemaModel current,
            TableModel incomingTable) {

        TableModel existingTable =
                current.getTables().get(incomingTable.getTableName());

        // Entire table is new
        if (existingTable == null) {
            return true;
        }

        // Existing table -> check its columns
        return hasNewColumn(existingTable, incomingTable);
    }

    private boolean hasNewColumn(
            TableModel existingTable,
            TableModel incomingTable) {

        // New or modified columns
        for (ColumnModel incomingColumn :
                incomingTable.getColumns().values()) {

            ColumnModel existingColumn =
                    existingTable.getColumns()
                            .get(incomingColumn.getColumnName());

            if (existingColumn == null) {
                return true;
            }

            if (!compareColumn(
                    existingColumn,
                    incomingColumn)) {
                return true;
            }
        }

        // Dropped columns
        for (String columnName :
                existingTable.getColumns().keySet()) {

            if (!incomingTable.getColumns().containsKey(columnName)) {
                return true;
            }
        }

        return false;
    }
}
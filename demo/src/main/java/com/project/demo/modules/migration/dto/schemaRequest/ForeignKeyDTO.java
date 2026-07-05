package com.project.demo.modules.migration.dto.schemaRequest;

import lombok.Data;

@Data
public class ForeignKeyDTO {
    // Column in the current table
    private String columnName;

    // Referenced table
    private String referencedTable;

    // Referenced column
    private String referencedColumn;

    // Optional: Constraint name
    private String constraintName;

    // Optional: Actions
    private String onDeleteAction;
    private String onUpdateAction;
}

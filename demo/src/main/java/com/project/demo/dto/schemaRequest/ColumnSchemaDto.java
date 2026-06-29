package com.project.demo.dto.schemaRequest;

import lombok.Data;

@Data
public class ColumnSchemaDto {
    private String columnName;

    private String dataType;

    private boolean nullable;

    private boolean primaryKey;

    private boolean foreignKey;

    private String referencedTable;

    private String referencedColumn;
}

package com.project.demo.BeforeExecutionValidation.model;


import lombok.Data;

@Data
public class ColumnModel {
    private String columnName;

    private String dataType;

    private boolean nullable = true;

    private boolean primaryKey;

    private boolean foreignKey;

    private boolean unique;

    private String defaultValue;

    private String referencedTable;

    private String referencedColumn;
}

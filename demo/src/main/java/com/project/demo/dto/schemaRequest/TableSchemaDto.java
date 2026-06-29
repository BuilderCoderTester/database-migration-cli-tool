package com.project.demo.dto.schemaRequest;

import lombok.Data;

import java.util.List;

@Data
public class TableSchemaDto {
    private String tableName;

    private List<ColumnSchemaDto> columns;

    private List<ForeignKeyDTO> foreignKeys;

    private List<PrimaryKeyDTO> primaryKeys;
}

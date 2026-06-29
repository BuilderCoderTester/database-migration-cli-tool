package com.project.demo.mappingProfile;

import com.project.demo.dto.schemaRequest.ColumnSchemaDto;
import com.project.demo.dto.schemaRequest.ForeignKeyDTO;
import com.project.demo.dto.schemaRequest.PrimaryKeyDTO;
import com.project.demo.dto.schemaRequest.TableSchemaDto;
import com.project.demo.model.Dependency;
import com.project.demo.enumuration.DependencyType;

import java.util.*;

public class DependencyMapper {

    public static List<TableSchemaDto> map(List<Dependency> dependencies) {

        Map<String, TableSchemaDto> tableMap = new LinkedHashMap<>();

        for (Dependency dependency : dependencies) {

            TableSchemaDto table = tableMap.computeIfAbsent(
                    dependency.getTable(),
                    t -> {
                        TableSchemaDto dto = new TableSchemaDto();
                        dto.setTableName(t);
                        dto.setColumns(new ArrayList<>());
                        dto.setForeignKeys(new ArrayList<>());
                        dto.setPrimaryKeys(new ArrayList<>());
                        return dto;
                    });

            switch (dependency.getType()) {

                case COLUMN -> {
                    ColumnSchemaDto column = new ColumnSchemaDto();
                    column.setColumnName(dependency.getColumn());

                    table.getColumns().add(column);
                }

                case PRIMARY_KEY -> {

                    PrimaryKeyDTO pk;

                    if (table.getPrimaryKeys().isEmpty()) {
                        pk = new PrimaryKeyDTO();
                        pk.setColumnNames(new ArrayList<>());
                        table.getPrimaryKeys().add(pk);
                    } else {
                        pk = table.getPrimaryKeys().get(0);
                    }

                    pk.getColumnNames().add(dependency.getColumn());
                }

                case FOREIGN_KEY -> {

                    ForeignKeyDTO fk = new ForeignKeyDTO();
                    fk.setColumnName(dependency.getColumn());
                    fk.setReferencedTable(dependency.getReferenceTable());
                    fk.setReferencedColumn(dependency.getReferenceColumn());

                    table.getForeignKeys().add(fk);
                }

                default -> {
                    // Ignore TABLE or other dependency types
                }
            }
        }

        return new ArrayList<>(tableMap.values());
    }
}
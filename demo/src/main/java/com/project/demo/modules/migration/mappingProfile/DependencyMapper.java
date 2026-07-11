package com.project.demo.modules.migration.mappingProfile;

import com.project.demo.modules.migration.dto.schemaRequest.ColumnSchemaDto;
import com.project.demo.modules.migration.dto.schemaRequest.ForeignKeyDTO;
import com.project.demo.modules.migration.dto.schemaRequest.PrimaryKeyDTO;
import com.project.demo.modules.migration.dto.schemaRequest.TableSchemaDto;
import com.project.demo.modules.migration.model.Dependency;

import java.util.*;

public class DependencyMapper {

    public static List<TableSchemaDto> map(List<Dependency> dependencies) {

        Map<String, TableSchemaDto> tableMap = new LinkedHashMap<>();

        for (Dependency dependency : dependencies) {

            if (dependency.getTable() == null) {
                continue;
            }

            TableSchemaDto table = tableMap.computeIfAbsent(
                    dependency.getTable(),
                    t -> {
                        TableSchemaDto dto = new TableSchemaDto();
                        dto.setTableName(t);
                        dto.setColumns(new ArrayList<>());
                        dto.setPrimaryKeys(new ArrayList<>());
                        dto.setForeignKeys(new ArrayList<>());
                        return dto;
                    });

            switch (dependency.getType()) {

                case TABLE:
                    // Nothing to add.
                    break;

                case COLUMN:

                    ColumnSchemaDto column = dependency.getColumn();

                    if (column != null) {
                        table.getColumns().add(column);
                    }

                    break;

                case PRIMARY_KEY:

                    PrimaryKeyDTO pk;

                    if (table.getPrimaryKeys().isEmpty()) {
                        pk = new PrimaryKeyDTO();
                        pk.setColumnNames(new ArrayList<>());
                        table.getPrimaryKeys().add(pk);
                    } else {
                        pk = table.getPrimaryKeys().get(0);
                    }

                    if (dependency.getColumnName() != null) {
                        pk.getColumnNames().add(
                                dependency.getColumnName()
                        );
                    }

                    break;

                case FOREIGN_KEY:

                    ForeignKeyDTO fk = dependency.getForeignKey();

                    if (fk != null) {
                        table.getForeignKeys().add(fk);
                    }

                    break;

                /*
                 * These are DML operations.
                 * They don't affect the schema model.
                 */
                case INSERT:
                case UPDATE:
                case DELETE:
                case ALTER:
                case DROP:
                case INDEX:
                case VIEW:
                case FUNCTION:
                case TRIGGER:
                case SEQUENCE:
                case VERSION:
                    break;

                default:
                    throw new IllegalStateException(
                            "Unsupported dependency type: "
                                    + dependency.getType()
                    );
            }
        }

        return new ArrayList<>(tableMap.values());
    }
}
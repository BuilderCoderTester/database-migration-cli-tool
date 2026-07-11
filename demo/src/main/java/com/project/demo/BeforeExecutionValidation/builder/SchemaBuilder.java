package com.project.demo.BeforeExecutionValidation.builder;

import com.project.demo.BeforeExecutionValidation.model.ColumnModel;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.model.TableModel;
import com.project.demo.BeforeExecutionValidation.parser.ASTSchemaExtractor;
import com.project.demo.modules.migration.model.MigrationScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class SchemaBuilder {

    @Autowired
    private ASTSchemaExtractor extractor;

    public SchemaModel build(List<MigrationScript> migrations)
            throws Exception {

        SchemaModel schema = new SchemaModel();

        migrations.stream()
                .sorted(Comparator.comparing(MigrationScript::getVersion))
                .forEach(script -> {

                    try {

                        SchemaModel parsed =
                                extractor.extract(script.getUpScript());

                        merge(schema, parsed);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                });

        return schema;
    }

    public SchemaModel build(MigrationScript migration)
            throws Exception {

        return extractor.extract(
                migration.getUpScript()
        );
    }
    private void merge(
            SchemaModel current,
            SchemaModel incoming) {

        for (Map.Entry<String, TableModel> entry :
                incoming.getTables().entrySet()) {

            String tableName = entry.getKey();
            TableModel incomingTable = entry.getValue();

            TableModel existing =
                    current.getTables().get(tableName);

            if (existing == null) {

                current.getTables().put(
                        tableName,
                        incomingTable
                );

            } else {

                mergeTable(existing, incomingTable);
            }
            if (incomingTable.isDropped()) {

                current.getTables().remove(tableName);

                continue;
            }
        }
    }

    private void mergeTable(
            TableModel current,
            TableModel incoming) {

        incoming.getColumns().forEach(
                (columnName, column) ->
                        current.getColumns().put(
                                columnName,
                                column
                        )
        );
    }
}
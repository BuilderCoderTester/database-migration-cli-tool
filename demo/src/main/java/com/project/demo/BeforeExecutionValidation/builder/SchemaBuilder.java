package com.project.demo.BeforeExecutionValidation.builder;

import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.parser.ASTSchemaExtractor;
import com.project.demo.modules.migration.model.MigrationScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class SchemaBuilder {

    @Autowired
    private ASTSchemaExtractor extractor;

    public SchemaModel build(List<MigrationScript> migrations) throws Exception {
//        System.out.println(Arrays.toString(migrations.toArray()));
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
    private void merge(
            SchemaModel current,
            SchemaModel incoming) {
//        System.out.println("the current : "+ current);
//        System.out.println("the current : "+ incoming);
        incoming.getTables().forEach((tableName, table) -> {

            current.getTables().putIfAbsent(
                    tableName,
                    table
            );

        });

    }
    public SchemaModel build(MigrationScript migration)
            throws Exception {

        return extractor.extract(
                migration.getUpScript()
        );

    }
}
package com.project.demo.BeforeExecutionValidation.validator;

import com.project.demo.BeforeExecutionValidation.builder.SchemaBuilder;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.model.TableModel;
import com.project.demo.modules.migration.dto.response.ValidationResult;
import com.project.demo.modules.migration.model.MigrationScript;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.delete.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeleteValidator {

    @Autowired
    private SchemaBuilder schemaBuilder;

    public ValidationResult validate(
            MigrationScript migration,
            List<MigrationScript> history) throws Exception {

        SchemaModel schema = schemaBuilder.build(history);

        // Parse the script into a Delete statement
        Delete delete = (Delete) CCJSqlParserUtil.parse(
                migration.getUpScript());

        if (delete.getTable() == null) {
            return ValidationResult.error("Invalid DELETE statement: Target table is missing.");
        }

        String tableName = delete.getTable().getName();
        TableModel table = schema.getTables().get(tableName);

        // 1. Validate table existence
        if (table == null) {
            return ValidationResult.error(
                    "Table '" + tableName + "' does not exist."
            );
        }

        return ValidationResult.success(
                "Delete statement validated successfully.",
                migration.getName()
        );
    }
}
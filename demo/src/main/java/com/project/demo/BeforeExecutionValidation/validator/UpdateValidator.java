package com.project.demo.BeforeExecutionValidation.validator;

import com.project.demo.BeforeExecutionValidation.builder.SchemaBuilder;
import com.project.demo.BeforeExecutionValidation.model.ColumnModel;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.model.TableModel;
import com.project.demo.modules.migration.dto.response.ValidationResult;
import com.project.demo.modules.migration.model.MigrationScript;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UpdateValidator {

    @Autowired
    private SchemaBuilder schemaBuilder;

    public ValidationResult validate(
            MigrationScript migration,
            List<MigrationScript> history) throws Exception {

        SchemaModel schema = schemaBuilder.build(history);

        // Parse the script into an Update statement
        Update update = (Update) CCJSqlParserUtil.parse(
                migration.getUpScript());

        if (update.getTable() == null) {
            return ValidationResult.error("Invalid UPDATE statement: Target table is missing.");
        }

        String tableName = update.getTable().getName();
        TableModel table = schema.getTables().get(tableName);

        // 1. Validate table existence
        if (table == null) {
            return ValidationResult.error(
                    "Table '" + tableName + "' does not exist."
            );
        }

        // 2. Validate targeted assignment columns
        if (update.getUpdateSets() != null) {
            for (UpdateSet updateSet : update.getUpdateSets()) {
                if (updateSet.getColumns() != null) {
                    for (var column : updateSet.getColumns()) {

                        ColumnModel existing = table.getColumns().get(column.getColumnName());

                        if (existing == null) {
                            return ValidationResult.error(
                                    "Column '" +
                                            column.getColumnName() +
                                            "' does not exist in table '" +
                                            tableName +
                                            "'."
                            );
                        }
                    }
                }
            }
        }

        return ValidationResult.success(
                "Update statement validated successfully.",
                migration.getName()
        );
    }
}
package com.project.demo.BeforeExecutionValidation.validator;

import com.project.demo.BeforeExecutionValidation.builder.SchemaBuilder;
import com.project.demo.BeforeExecutionValidation.model.ColumnModel;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.model.TableModel;
import com.project.demo.modules.migration.dto.response.ValidationResult;
import com.project.demo.modules.migration.model.MigrationScript;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.insert.Insert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InsertValidator {

    @Autowired
    private SchemaBuilder schemaBuilder;

    public ValidationResult validate(
            MigrationScript migration,
            List<MigrationScript> history) throws Exception {

        SchemaModel schema = schemaBuilder.build(history);

        Insert insert = (Insert) CCJSqlParserUtil.parse(
                migration.getUpScript());

        String tableName =
                insert.getTable().getName();

        TableModel table =
                schema.getTables().get(tableName);

        if (table == null) {

            return ValidationResult.error(
                    "Table '" + tableName + "' does not exist."
            );
        }

        if (insert.getColumns() != null) {

            for (var column : insert.getColumns()) {

                ColumnModel existing =
                        table.getColumns().get(column.getColumnName());

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

        return ValidationResult.success(
                "Insert statement validated successfully.",
                migration.getName()
        );
    }
}
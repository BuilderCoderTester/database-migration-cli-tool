package com.project.demo.modules.migration.service;

import com.project.demo.BeforeExecutionValidation.SchemaValidatorService;
import com.project.demo.BeforeExecutionValidation.validator.InsertValidator;
import com.project.demo.BeforeExecutionValidation.validator.UpdateValidator;
import com.project.demo.BeforeExecutionValidation.validator.DeleteValidator;
import com.project.demo.modules.migration.dto.response.ValidationResult;
import com.project.demo.modules.migration.model.MigrationScript;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MigrationValidatorService {

    @Autowired
    private SchemaValidatorService schemaValidatorService;
    @Autowired
    private UpdateValidator updateValidator;

    @Autowired
    private DeleteValidator deleteValidator;
    @Autowired
    private InsertValidator insertValidator;

    public ValidationResult validate(
            MigrationScript migration,
            List<MigrationScript> history) throws Exception {

        Statement statement =
                CCJSqlParserUtil.parse(
                        migration.getUpScript()
                );

        // Schema operations
        if (statement instanceof CreateTable ||
                statement instanceof Alter ||
                statement instanceof Drop) {

            return schemaValidatorService.validate(
                    migration,
                    history
            );
        }

        // Data operations
        if (statement instanceof Insert) {

            return insertValidator.validate(
                    migration,
                    history
            );
        }

        if (statement instanceof Update) {

            return updateValidator.validate(
                    migration,
                    history
            );
        }

        if (statement instanceof Delete) {

            return deleteValidator.validate(
                    migration,
                    history
            );
        }

        return ValidationResult.success(
                "No validation required.",
                migration.getName()
        );
    }
}
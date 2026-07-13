package com.project.demo.modules.migration.service;

import com.project.demo.BeforeExecutionValidation.SchemaValidatorService;
import com.project.demo.BeforeExecutionValidation.validator.InsertValidator;
import com.project.demo.modules.migration.dto.response.ValidationResult;
import com.project.demo.modules.migration.model.MigrationScript;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.insert.Insert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MigrationValidatorService {

    @Autowired
    private SchemaValidatorService schemaValidatorService;

    @Autowired
    private InsertValidator insertValidator;

    public ValidationResult validate(
            MigrationScript migration,
            List<MigrationScript> history) throws Exception {

        String sql = migration.getUpScript();

        Statement statement = CCJSqlParserUtil.parse(sql);

        if (statement instanceof CreateTable ||
                statement instanceof Alter) {

            return schemaValidatorService.validate(
                    migration,
                    history
            );
        }

        if (statement instanceof Insert) {

            return insertValidator.validate(
                    migration,
                    history
            );
        }

        return ValidationResult.success(
                "Validation skipped.",
                migration.getName()
        );
    }
}
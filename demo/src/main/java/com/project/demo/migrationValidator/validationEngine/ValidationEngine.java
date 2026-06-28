package com.project.demo.migrationValidator.validationEngine;

import com.project.demo.migrationValidator.interfaces.MigrationValidator;
import com.project.demo.model.MigrationScript;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ValidationEngine {

    private final List<MigrationValidator> validators;

    public void validate(MigrationScript script, Connection connection) {

        for (MigrationValidator validator : validators) {

            validator.validate(script,connection);

        }

    }
}

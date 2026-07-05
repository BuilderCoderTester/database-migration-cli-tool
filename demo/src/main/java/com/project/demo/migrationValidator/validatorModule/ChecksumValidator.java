package com.project.demo.migrationValidator.validatorModule;

import com.project.demo.migrationValidator.exception.ValidationException;
import com.project.demo.migrationValidator.interfaces.MigrationValidator;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.utility.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
@RequiredArgsConstructor
public class ChecksumValidator implements MigrationValidator {
    private final Helper helper;

    @Override
    public void validate(MigrationScript script, Connection connection) throws ValidationException {

        try {
            boolean valid =
                    helper.validateChecksum(
                            script.getVersion(),
                            script.getUpScript());
            if (!valid) {

                throw new ValidationException(
                        "Checksum validation failed.");

            }

        } catch (Exception e) {
            throw new ValidationException("Checksum validation failed.");
        }

    }
}

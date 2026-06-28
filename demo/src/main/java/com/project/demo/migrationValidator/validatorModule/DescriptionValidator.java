package com.project.demo.migrationValidator.validatorModule;

import com.project.demo.migrationValidator.exception.ValidationException;
import com.project.demo.migrationValidator.interfaces.MigrationValidator;
import com.project.demo.model.MigrationScript;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Set;

@Component
public class DescriptionValidator implements MigrationValidator {
    private static final Set<String> VALID_OPERATIONS = Set.of(
            "create", "alter", "drop", "rename",
            "add", "remove", "modify","insert"
    );

    private static final Set<String> VALID_TARGETS = Set.of(
            "table", "column", "index",
            "constraint", "view", "trigger",
            "function", "procedure","into"
    );

    @Override
    public void validate(MigrationScript script, Connection connection) throws ValidationException {

        String description = script.getDescription();
        System.out.println(description);
        if (description == null || description.isBlank()) {
            throw new ValidationException("Migration description is missing.");
        }

        String[] parts = description.trim().split("_");

        if (parts.length < 3) {
            throw new ValidationException(
                    "Description must follow the format: <operation>_<target>_<name>."
            );
        }

        String operation = parts[0].toLowerCase();
        String target = parts[1].toLowerCase();

        if (!VALID_OPERATIONS.contains(operation)) {
            throw new ValidationException(
                    "Invalid operation '" + operation + "'."
            );
        }

        if (!VALID_TARGETS.contains(target)) {
            throw new ValidationException(
                    "Invalid target '" + target + "'."
            );
        }

        String name = String.join("_",
                java.util.Arrays.copyOfRange(parts, 2, parts.length));

        if (name.isBlank()) {
            throw new ValidationException(
                    "Object name is missing in the description."
            );
        }
    }
}

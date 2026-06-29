package com.project.demo.BeforeExecutionValidation;

import com.project.demo.BeforeExecutionValidation.builder.SchemaBuilder;
import com.project.demo.BeforeExecutionValidation.comparator.SchemaComparator;
import com.project.demo.dto.response.ValidationResult;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.model.MigrationScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchemaValidatorService {
    @Autowired
    private SchemaBuilder schemaBuilder;

    @Autowired
    private SchemaComparator comparator;

    public ValidationResult validate(
            MigrationScript newMigration,
            List<MigrationScript> history)
            throws Exception {

        SchemaModel current =
                schemaBuilder.build(history);

        SchemaModel incoming =
                schemaBuilder.build(newMigration);
        if (comparator.isDuplicate(current, incoming)) {

            return ValidationResult.error(
                    "An identical migration already exists."
            );
        }

        if (!comparator.introducesNewChanges(
                current,
                incoming)) {

            return ValidationResult.error(
                    "Migration introduces no new schema changes."
            );
        }

        return ValidationResult.success(
                "Validation successful."
        );

    }

}

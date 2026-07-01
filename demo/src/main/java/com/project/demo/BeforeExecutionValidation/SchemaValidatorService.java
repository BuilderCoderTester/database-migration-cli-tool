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
//        System.out.println("Other scripts : "+current.toString());
        SchemaModel incoming =
                schemaBuilder.build(newMigration);
//        System.out.println("New created Script: " + incoming.toString());
        boolean value = comparator.isDuplicate(current, incoming);
        if (value) {
            return ValidationResult.error(
                    "An identical migration already exists."
            );
        }
        boolean val = !comparator.introducesNewChanges(
                current,
                incoming);
        System.out.println("the boolean value is " + val);

        if (val) {

            return ValidationResult.error(
                    "Migration introduces no new schema changes."
            );
        }
        System.out.println("reach point schema service -1");

        return ValidationResult.success(
                "Validation successful."
        );

    }

}

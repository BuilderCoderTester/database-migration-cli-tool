package com.project.demo.migrationRepair.repairModule;

import com.project.demo.migrationRepair.interfaces.MigrationRepairer;
import com.project.demo.model.MigrationScript;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
public class DescriptionRepairer implements MigrationRepairer {
    @Override
    public void repair(MigrationScript script, Connection connection) {

        if (script.getDescription() == null ||
                script.getDescription().isBlank()) {

            String fileName = script.getFileName();

            if (fileName != null && fileName.contains("__")) {

                String description =
                        fileName.substring(fileName.indexOf("__") + 2)
                                .replace(".sql", "")
                                .replace("_", " ");

                script.setDescription(description);
            }
        }

    }
}

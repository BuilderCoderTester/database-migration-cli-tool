package com.project.demo.migrationRepair.repairModule;

import com.project.demo.migrationRepair.exception.RepairException;
import com.project.demo.migrationRepair.interfaces.MigrationRepairer;
import com.project.demo.modules.migration.model.MigrationScript;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
public class SqlFormatterRepairer implements MigrationRepairer {
    @Override
    public void repair(MigrationScript script, Connection connection) throws RepairException {
        if (script.getFileName() == null ||
                script.getFileName().isBlank()) {

            script.setFileName(
                    script.getVersion()
                            + "__"
                            + script.getDescription().replace(" ", "_")
                            + ".sql");

        }
    }
}

package com.project.demo.migrationRepair.repairModule;

import com.project.demo.migrationRepair.exception.RepairException;
import com.project.demo.migrationRepair.interfaces.MigrationRepairer;
import com.project.demo.modules.migration.model.MigrationScript;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
public class VersionRepairer implements MigrationRepairer {
    @Override
    public void repair(MigrationScript script, Connection connection) throws RepairException {
        if (script.getVersion() == null ||
                script.getVersion().isBlank()) {

            script.setVersion("V1");
            return;
        }

        if (!script.getVersion().startsWith("V")) {

            script.setVersion("V" + script.getVersion());

        }
    }
}

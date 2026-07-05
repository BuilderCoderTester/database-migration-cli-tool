package com.project.demo.migrationRepair.engine;

import com.project.demo.migrationRepair.interfaces.MigrationRepairer;
import com.project.demo.modules.migration.model.MigrationScript;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RepairEngine {
    private final List<MigrationRepairer> repairers;

    public void repair(MigrationScript script, Connection connection) {

        for (MigrationRepairer repairer : repairers) {
            repairer.repair(script,connection);
        }

    }
}

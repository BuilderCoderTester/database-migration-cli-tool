package com.project.demo.migrationRepair.interfaces;

import com.project.demo.migrationRepair.exception.RepairException;
import com.project.demo.modules.migration.model.MigrationScript;

import java.sql.Connection;

public interface MigrationRepairer {
    void repair(MigrationScript script , Connection connection) throws RepairException;
}

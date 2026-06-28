package com.project.demo.migrationRepair.repairModule;

import com.project.demo.migrationRepair.exception.RepairException;
import com.project.demo.migrationRepair.interfaces.MigrationRepairer;
import com.project.demo.model.MigrationScript;
import com.project.demo.utility.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
@RequiredArgsConstructor
public class ChecksumRepairer implements MigrationRepairer {

    private final Helper helper;
    @Override
    public void repair(MigrationScript script, Connection connection) throws RepairException {
        helper.updateChecksum(
                script.getVersion(),
                script.getUpScript());
    }
}

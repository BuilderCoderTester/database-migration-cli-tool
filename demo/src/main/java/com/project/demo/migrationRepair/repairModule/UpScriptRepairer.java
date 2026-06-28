package com.project.demo.migrationRepair.repairModule;

import com.project.demo.migrationRepair.exception.RepairException;
import com.project.demo.migrationRepair.interfaces.MigrationRepairer;
import com.project.demo.model.MigrationScript;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
public class UpScriptRepairer implements MigrationRepairer {
    @Override
    public void repair(MigrationScript script, Connection connection) throws RepairException {
        if (!script.isRepeatable()) {

            if (script.getDownScript() == null ||
                    script.getDownScript().trim().isEmpty()) {

                script.setDownScript("""
                        -- TODO:
                        -- Write your DOWN SQL here
                        """);

            }

        }
    }
}

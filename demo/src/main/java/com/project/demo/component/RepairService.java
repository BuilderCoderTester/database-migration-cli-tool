package com.project.demo.component;

import com.project.demo.modules.migration.model.MigrationScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RepairService {
    @Autowired
    private MigrationRepair migrationRepair;

    public MigrationScript repair(MigrationScript failed, Long connectionId) throws Exception {
       return migrationRepair.migrationRepairFlow(failed,connectionId);
    }
}

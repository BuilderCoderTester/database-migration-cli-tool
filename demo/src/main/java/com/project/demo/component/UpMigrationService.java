package com.project.demo.component;

import com.project.demo.enumuration.Status;
import com.project.demo.model.MigrationScript;
import com.project.demo.utility.Helper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

@Service
public class UpMigrationService {
    @Autowired
    private Helper helper;

    public void upMigrationScript(MigrationScript script, long connectionId, long startTime) throws SQLException {
        helper.applyVersioned(script,connectionId,startTime);
    }

    public void saveMigrationRecord(MigrationScript script, long connectionId, long startTime, boolean value , Connection connection) throws SQLException {
            helper.saveMigrationRecord(script, connectionId, System.currentTimeMillis() - startTime, false,connection);

    }

    public void updateMigrationScript(MigrationScript scriptVersion,long startTime ,long connectionId, Status status, long executionTime) throws SQLException {
                    helper.updateMigrationStatus(scriptVersion.getVersion(), connectionId, Status.PASSED,
                    System.currentTimeMillis() - startTime);
    }

}

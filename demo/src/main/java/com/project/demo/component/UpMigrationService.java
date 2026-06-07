package com.project.demo.component;

import com.project.demo.model.MigrationScript;
import com.project.demo.utility.Helper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class UpMigrationService {
    @Autowired
    private Helper helper;

    public void upMigrationScript(MigrationScript script, long connectionId, long startTime) throws SQLException {
        helper.applyVersioned(script,connectionId,startTime);
    }
}

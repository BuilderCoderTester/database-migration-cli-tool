package com.project.demo.component;

import com.project.demo.dto.MigrationScriptStatus;
import com.project.demo.enumuration.Status;
import com.project.demo.model.Dependency;
import com.project.demo.model.MigrationLogs;
import com.project.demo.model.MigrationScript;
import com.project.demo.parser.ASTDependencyExtractor;
import com.project.demo.repository.MigrationRepository;
import com.project.demo.service.ConnectionService;
import com.project.demo.service.MigrationFailureService;
import com.project.demo.validator.DependencyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Component
public class MigrationOrchestrator {

    @Autowired
    private UpMigrationService upService;
    @Autowired
    private DownMigrationService downService;
    @Autowired
    private RepeatableMigrationService repeatableService;
    @Autowired
    private MigrationRepository repository;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private DependencyEngine dependencyEngine;
    @Autowired
    private RepairService repairService;
    @Autowired
    private MigrationFailureService failureService;
    private static final Logger logger = LoggerFactory.getLogger(MigrationEngine.class);

    public void initialize() {
        repository.createSchemaHistoryTable();
        logger.info("Schema history table initialized");
    }

    // MIGRATE UP MODULE
    @Transactional(rollbackFor = Exception.class)
    public void migrateUp(MigrationScript script, Long connectionId, String currentDatabase) throws SQLException {
        System.out.println("reach point -1 ");
        if (connectionId == null) {
            throw new RuntimeException("No active connection selected");
        }

        long startTime = System.currentTimeMillis();

        System.out.println("Applying migration: "
                + script.getVersion() + " - " + script.getDescription()
                + " [connectionId=" + connectionId + "]");

        try {
            Connection conn = connectionService.activeConnection(currentDatabase);
//            helper.saveMigrationRecord(script, connectionId, System.currentTimeMillis() - startTime, false,conn);

            // has bugs (workings.............)
//            validator.validateBeforeUp(script);

            MigrationScriptStatus scriptStatus = dependencyEngine.validate(script,conn);
            // 2. 🔥 AST Dependency Extraction
//            ASTDependencyExtractor extractor = new ASTDependencyExtractor();
//            List<Dependency> deps = extractor.extract(script.getUpScript());
//            System.out.println("Dependency : " + deps);
//
//            // only checked for CREATE not for others
//            DependencyValidator validator = new DependencyValidator();
//            MigrationScriptStatus scriptStatus = validator.validate(deps, conn);
            System.out.println("Status = " + scriptStatus.getTableName());
            // if failed then call the repair function .
            if(scriptStatus.getStatus() == Status.FAILURE){
                System.out.println("have it here bro");
                MigrationScript currentScript =  repairService.repair(script,connectionId);
                System.out.println("The required script is "+ currentScript.toString());
                upService.upMigrationScript(currentScript,startTime,connectionId);
            }
            if (script.isRepeatable()) {
                // not done yet (working ............)
//                applyRepeatable(script, connectionId, startTime);
            } else {
                upService.upMigrationScript(script, startTime, connectionId);
            }
            // 2. ✅ UPDATE TO SUCCESS
//            helper.updateMigrationStatus(script.getVersion(), connectionId, Status.PASSED,
//                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            failureService.logFailure(script, e, connectionId); // Log in separate transaction
            throw new RuntimeException("Migration failed: " + script.getVersion(), e);
        }
    }

    @Transactional
    public boolean migrateDown(MigrationScript script, String currentDatabase) {

        if (script.getDownScript() == null || script.getDownScript().isBlank()) {
            logger.warn("No down script available for version {}", script.getVersion());
            return false;
        }

        logger.info("Reverting migration: {} - {}",
                script.getVersion(),
                script.getDescription());

        String deleteMigrationSql = """
            DELETE FROM sub_migration
            WHERE version = ?
            """;

        try (
                Connection connection =
                        connectionService.activeConnection(currentDatabase)
        ) {

            connection.setAutoCommit(false);

            // 🔥 Execute DOWN script
            try (PreparedStatement statement =
                         connection.prepareStatement(script.getDownScript())) {

                statement.executeUpdate();
            }

            // 🔥 Remove migration history
            try (PreparedStatement deleteStmt =
                         connection.prepareStatement(deleteMigrationSql)) {

                deleteStmt.setString(1, script.getVersion());

                deleteStmt.executeUpdate();
            }

            connection.commit();

            logger.info("Migration reverted successfully");

            return true;

        } catch (Exception e) {

            logger.error("Rollback failed for version {}: {}",
                    script.getVersion(),
                    e.getMessage(),
                    e);

            return false;
        }
    }

}

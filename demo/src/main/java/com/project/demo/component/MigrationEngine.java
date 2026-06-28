// MigrationEngine.java
package com.project.demo.component;

import com.project.demo.dto.MigrationScriptStatus;
import com.project.demo.enumuration.Status;
import com.project.demo.model.Dependency;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.parser.ASTDependencyExtractor;
import com.project.demo.repository.MigrationRepository;
import com.project.demo.service.ConnectionService;
import com.project.demo.service.MigrationFailureService;
import com.project.demo.utility.Helper;
import com.project.demo.validator.DependencyValidator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

@Component
@AllArgsConstructor
@Slf4j
public class MigrationEngine {

    private static final Logger logger = LoggerFactory.getLogger(MigrationEngine.class);

    private final MigrationRepository repository;
    private final MigrationFailureService failureService;
    private final MigrationValidator validator;
    private final SqlExecutor sqlExecutor;
    private final Helper helper;
    private final ConnectionService connectionService;
    private final MigrationRepair migrationRepair;
    public void initialize() {
        repository.createSchemaHistoryTable();
        logger.info("Schema history table initialized");
    }

    // MIGRATE UP MODULE
    @Transactional(rollbackFor = Exception.class)
    public void migrateUp(MigrationScript script, Long connectionId,String currentDatabase) throws SQLException {
        log.debug("Starting migrate up for script {} on connection {}", script.getVersion(), connectionId);
        if (connectionId == null) {
            throw new RuntimeException("No active connection selected");
        }

        long startTime = System.currentTimeMillis();

        log.info("Applying migration {} - {} [connectionId={}]", script.getVersion(), script.getDescription(), connectionId);

        try {
            Connection conn = helper.activeConnection(currentDatabase);
//            helper.saveMigrationRecord(script, connectionId, System.currentTimeMillis() - startTime, false,conn);
            log.debug("Active migration connection opened for database {}", currentDatabase);
            // has bugs (workings.............)
//            validator.validateBeforeUp(script);

            // 2. 🔥 AST Dependency Extraction
            ASTDependencyExtractor extractor = new ASTDependencyExtractor();
            List<Dependency> deps = extractor.extract(script.getUpScript());
            log.debug("Extracted dependencies for {}: {}", script.getVersion(), deps);

            // only checked for CREATE not for others
            DependencyValidator validator = new DependencyValidator();
            MigrationScriptStatus scriptStatus = validator.validate(deps, conn);
            log.debug("Dependency validation status for table {}: {}", scriptStatus.getTableName(), scriptStatus.getStatus());
            // if failed then call the repair function .
            if(scriptStatus.getStatus() == Status.FAILURE){
                log.error(
                        "Migration {} failed validation. Reason: {}",
                        script.getVersion(),
                        scriptStatus.getReason()
                );

                MigrationScript currentScript =  migrationRepair.migrationRepairFlow(script,connectionId);
                log.info("Applying repaired migration script for {}", script.getVersion());
                helper.applyVersioned(currentScript,startTime,connectionId);
            }
            if (script.isRepeatable()) {
                // not done yet (working ............)
                applyRepeatable(script, connectionId, startTime);
            } else {
                helper.applyVersioned(script, startTime, connectionId);
            }
            // 2. ✅ UPDATE TO SUCCESS
//            helper.updateMigrationStatus(script.getVersion(), connectionId, Status.PASSED,
//                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            failureService.logFailure(script, e, connectionId); // Log in separate transaction
            throw new RuntimeException("Migration failed: " + script.getVersion(), e);
        }
    }

    private void applyRepeatable(MigrationScript script, long start, Long connectionId) {

        String checksum = helper.calculateChecksum(script.getUpScript());

        Optional<Migration> existing = repository.findById(script.getVersion());

        // run if new OR checksum changed
        if (existing.isEmpty() ||
                !checksum.equals(existing.get().getChecksum())) {

            logger.info("Running repeatable: {}", script.getVersion());

//            sqlExecutor.executeScript(script.getUpScript());

//            helper.saveMigrationRecord(script, connectionId,System.currentTimeMillis() - start, true);

        } else {
            logger.info("Skipping repeatable (no changes): {}", script.getVersion());
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
                        helper.activeConnection(currentDatabase)
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

//    public void retryFailedMigration(String version) {
//
//        Optional<Migration> failed = repository.findById(version);
//
//        if (failed.isEmpty()) {
//            throw new RuntimeException("No failed migration found for version: " + version);
//        }
//
//        Migration m = failed.get();
//
//        if (m.isSuccess()) {
//            throw new RuntimeException("Migration is not failed: " + version);
//        }
//
//        MigrationScript script = new MigrationScript(
//                m.getVersion(),
//                m.getDescription(),
//                m.getScript()
//        );
//
//        // 🔴 Clear dirty BEFORE retry
//        repository.clearDirtyFlag(version);
//
//        // 🔁 Re-run migration
//        migrateUp(script);
//    }

}

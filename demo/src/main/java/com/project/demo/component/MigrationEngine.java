// MigrationEngine.java
package com.project.demo.component;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.util.*;

@Component
@AllArgsConstructor
public class MigrationEngine {

    private static final Logger logger = LoggerFactory.getLogger(MigrationEngine.class);

    private final MigrationRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final MigrationFailureService failureService;
    private final MigrationValidator validator;
    private final SqlExecutor sqlExecutor;
    private final Helper helper;
    private final ConnectionService connectionService;

    public void initialize() {
        repository.createSchemaHistoryTable();
        logger.info("Schema history table initialized");
    }

    // MIGRATE UP MODULE
    @Transactional(rollbackFor = Exception.class)
    public void migrateUp(MigrationScript script ,Long connectionId) {

        if (connectionId == null) {
            throw new RuntimeException("No active connection selected");
        }

        long startTime = System.currentTimeMillis();

        System.out.println("Applying migration: "
                + script.getVersion() + " - " + script.getDescription()
                + " [connectionId=" + connectionId + "]");
        try {
            Connection conn = connectionService.getConnection(connectionId);
//            System.out.println("the conn :"+conn);
//            System.out.println("the real conneciton " + conn);
            // has bugs (workings.............)
//            validator.validateBeforeUp(script);

            // 2. 🔥 AST Dependency Extraction
//            ASTDependencyExtractor extractor = new ASTDependencyExtractor();
//            List<Dependency> deps = extractor.extract(script.getUpScript());
//
//            DependencyValidator validator = new DependencyValidator();
//            validator.validate(deps, conn);

            if (script.isRepeatable()) {
                // not done yet (working ............)
                applyRepeatable(script, connectionId,startTime);
            } else {
                helper.applyVersioned(script, startTime, connectionId);
            }

        } catch (Exception e) {
            failureService.logFailure(script, e,connectionId); // Log in separate transaction
            throw new RuntimeException("Migration failed: " + script.getVersion(), e);
        }
    }

    private void applyRepeatable(MigrationScript script, long start ,Long connectionId) {

        String checksum = helper.calculateChecksum(script.getUpScript());

        Optional<Migration> existing = repository.findById(script.getVersion());

        // run if new OR checksum changed
        if (existing.isEmpty() ||
                !checksum.equals(existing.get().getChecksum())) {

            logger.info("Running repeatable: {}", script.getVersion());

            sqlExecutor.executeScript(script.getUpScript());

            helper.saveMigrationRecord(script, connectionId,System.currentTimeMillis() - start, true);

        } else {
            logger.info("Skipping repeatable (no changes): {}", script.getVersion());
        }
    }

    @Transactional
    public boolean migrateDown(MigrationScript script) {
        if (script.getDownScript() == null || script.getDownScript().isEmpty()) {
            logger.warn("No down script available for version {}", script.getVersion());
            return false;
        }

        try {
            logger.info("Reverting migration: {} - {}", script.getVersion(), script.getDescription());

            jdbcTemplate.execute(script.getDownScript());
            repository.deleteByVersion(script.getVersion());

            logger.info("Migration reverted successfully");
            return true;

        } catch (Exception e) {
            logger.error("Rollback failed: {}", e.getMessage());
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
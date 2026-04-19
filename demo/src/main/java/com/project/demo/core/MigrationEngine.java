// MigrationEngine.java
package com.project.demo.core;

import com.project.demo.component.MigrationValidator;
import com.project.demo.component.SqlExecutor;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.MigrationRepository;
import com.project.demo.service.ChecksumService;
import com.project.demo.service.MigrationFailureService;
import com.project.demo.service.MigrationLockService;
import com.project.demo.utility.Helper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
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
    private final ChecksumService checksumService;

    public void initialize() {
        repository.createSchemaHistoryTable();
        logger.info("Schema history table initialized");
    }

    @Transactional(rollbackFor = Exception.class)
    public void migrateUp(MigrationScript script) {

        long startTime = System.currentTimeMillis();

        logger.info("Applying migration: {} - {}", script.getVersion(), script.getDescription());

        try {
            // 1.validate
            validator.validateBeforeUp(script);

            if (script.isRepeatable()) {
                applyRepeatable(script, startTime);
            } else {
                applyVersioned(script, startTime);
            }

        } catch (Exception e) {
            failureService.logFailure(script, e); // Log in separate transaction
            throw new RuntimeException("Migration failed: " + script.getVersion(), e);
        }
    }

    private void applyVersioned(MigrationScript script, long start) {

        sqlExecutor.executeScript(script.getUpScript());

        saveMigrationRecord(script, System.currentTimeMillis() - start, false);
    }

    private void applyRepeatable(MigrationScript script, long start) {

        String checksum = calculateChecksum(script.getUpScript());

        Optional<Migration> existing = repository.findById(script.getVersion());

        // run if new OR checksum changed
        if (existing.isEmpty() ||
                !checksum.equals(existing.get().getChecksum())) {

            logger.info("Running repeatable: {}", script.getVersion());

            sqlExecutor.executeScript(script.getUpScript());

            saveMigrationRecord(script, System.currentTimeMillis() - start, true);

        } else {
            logger.info("Skipping repeatable (no changes): {}", script.getVersion());
        }
    }

    private void saveMigrationRecord(MigrationScript script,
                                     long executionTime,
                                     boolean repeatable) {

        Migration m = new Migration(
                script.getVersion(),
                script.getDescription(),
                script.getUpScript()
        );

        m.setChecksum(calculateChecksum(script.getUpScript()));
        m.setExecutedAt(LocalDateTime.now());
        m.setExecutionTime(executionTime);
        m.setSuccess(true);
        m.setDirty(false);
        m.setRepeatable(repeatable);
        m.setName(script.getName());

        repository.save(m);
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

    public void retryFailedMigration(String version) {

        Optional<Migration> failed = repository.findById(version);

        if (failed.isEmpty()) {
            throw new RuntimeException("No failed migration found for version: " + version);
        }

        Migration m = failed.get();

        if (m.isSuccess()) {
            throw new RuntimeException("Migration is not failed: " + version);
        }

        MigrationScript script = new MigrationScript(
                m.getVersion(),
                m.getDescription(),
                m.getScript()
        );

        // 🔴 Clear dirty BEFORE retry
        repository.clearDirtyFlag(version);

        // 🔁 Re-run migration
        migrateUp(script);
    }

    public boolean validateChecksum(String version, String script) {
        Optional<Migration> existing = repository.findByVersion(version);
        if (existing.isEmpty()) return true;

        String currentChecksum = calculateChecksum(script);
        return currentChecksum.equals(existing.get().getChecksum());
    }

    private String calculateChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public Optional<String> getCurrentVersion() {
        return repository.findLastSuccessful().map(Migration::getVersion);
    }

    public List<Migration> getMigrationHistory() {
        return repository.findAll();
    }
}
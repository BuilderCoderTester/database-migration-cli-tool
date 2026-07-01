package com.project.demo.service;

import com.project.demo.BeforeExecutionValidation.builder.DatabaseSchemaBuilder;
import com.project.demo.BeforeExecutionValidation.comparator.SchemaComparator;
import com.project.demo.BeforeExecutionValidation.parser.ASTSchemaExtractor;
import com.project.demo.component.*;
import com.project.demo.dto.MigrationResult;
import com.project.demo.dto.StatusResponse;
import com.project.demo.dto.request.MigrationRequest;
import com.project.demo.migrationRepair.engine.RepairEngine;
import com.project.demo.migrationValidator.exception.ValidationException;
import com.project.demo.migrationValidator.validationEngine.ValidationEngine;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.MigrationRepository;
import com.project.demo.utility.Helper;
import com.project.demo.utility.SqlOperationDetector;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class MigrationLifecycleService {

    @Autowired
    private MigrationLockService migrationLockService;
    @Autowired
    private Helper helper;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ConnectionContext connectionContext;
    @Autowired
    private MigrationEngine engine;
    @Autowired
    private MigrationLoader loader;
    @Autowired
    private SqlOperationDetector sqlOperationDetector;
    @Autowired
    private MigrationRepository migrationRepository;
    @Autowired
    private SchemaDiffGenerator schemaDiffGenerator;
    @Autowired
    private ValidationEngine validationEngine;
    @Autowired
    private RepairEngine repairEngine;

    @Autowired
    private DatabaseSchemaBuilder databaseSchemaBuilder;

    @Autowired
    private ASTSchemaExtractor astSchemaExtractor;

    @Autowired
    private SchemaComparator schemaComparator;
    /**
     * Execute the Migration Script : Partially Done.
     */
    @Transactional
    public MigrationResult migrate(MigrationRequest migrationRequest) throws SQLException {

        // Database validation checking
        Long connectionId = migrationRequest.getConnectionId();
        String targetVersion = migrationRequest.getTargetVersion();
        log.info("Starting migration for connection {}", connectionId);

        String lockedBy = null;

        if (connectionId == null) {
            throw new RuntimeException("No active connection selected");
        }
        System.out.println("reach point -2");

        Connection connection = connectionService.activeConnection(connectionContext.getCurrentDatabase());
        connection.setAutoCommit(false);
        System.out.println("reach point -3");

        try {
            //ACQUIRE LOCK
            if (migrationLockService.isLockStale(connection)) {

                log.warn("Stale migration lock detected for connection {}", connectionId);

                migrationLockService.clearStaleLock(connection);
            }
            //lock accuire
            lockedBy = migrationLockService.acquireLock(connection, connectionId);
            migrationLockService.updateHeartbeat(connection);
//            var currentOpt = helper.getCurrentVersion(connectionId, connectionContext.getCurrentDatabase());
            Set<String> executedVersions = helper.getExecutedVersions(connectionId, connectionContext.getCurrentDatabase());

//            System.out.println("the currentOPT " + currentOpt);
//            List<MigrationScript> pending =
//                    loader.loadPendingMigrations(currentOpt.orElse(null), connectionId);
            List<MigrationScript> pending = loader.loadPendingMigrations(executedVersions, connectionId);

            for (MigrationScript sc : pending) {
                log.debug("Pending migration script {}", sc.getVersion());
            }
            if (pending.isEmpty()) {
                return new MigrationResult("✓ No pending migrations", 0, 0);
            }

            int success = 0;
            int failed = 0;
            List<String> applied = new ArrayList<>();

            for (MigrationScript script : pending) {
                migrationRequest.setOperation(sqlOperationDetector.detectOperation(script.getDescription()));
                log.debug("Detected operation {} for migration {}", migrationRequest.getOperation(), script.getVersion());
                if (targetVersion != null && helper.compareVersion(script.getVersion(), targetVersion) > 0) {
                    break;
                }

                try {
                    // 🔥 run migration on correct DB
                    engine.migrateUp(script, connectionId, connectionContext.getCurrentDatabase());
                    success++;
                    applied.add(script.getVersion());

                } catch (Exception e) {

                    failed++;

                    // 🔥 save failure
                    migrationRepository.saveFailure(script, connectionId, e);

                    return new MigrationResult(String.format("✗ Migration failed at %s\nReason: %s\nApplied: %s", script.getVersion(), e.getMessage(), applied), success, failed);
                }
            }

            return new MigrationResult(String.format("✓ Migration complete\nApplied: %s\nSuccess: %d, Failed: %d", applied, success, failed), success, failed);

        } catch (IOException e) {
            log.error("Failed to load migrations for connection {}", connectionId, e);
            return new MigrationResult("✗ Migration failed: " + e.getMessage(), 0, 0);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                migrationLockService.releaseLock(connection, connectionId, lockedBy); // 🔥 scoped unlock
                connection.commit();
            } catch (Exception e) {
                log.warn("Failed to release migration lock cleanly for connection {}", connectionId, e);
            }
        }
    }

    /**
     * Execute the Migration Script by Version : Partially Done.
     */
    @Transactional
    public MigrationResult migrateSingle(MigrationRequest request) throws SQLException {
        System.out.println("the version in here" + request.getTargetVersion());
        Long connectionId = request.getConnectionId();
        String targetVersion = request.getTargetVersion();

        if (connectionId == null) {
            throw new RuntimeException("No active connection selected.");
        }

        if (targetVersion == null || targetVersion.isBlank()) {
            throw new RuntimeException("Target version is required.");
        }

        Connection connection =
                connectionService.activeConnection(connectionContext.getCurrentDatabase());

        connection.setAutoCommit(false);

        String lockedBy = null;

        try {

            // Handle stale lock
            if (migrationLockService.isLockStale(connection)) {
                migrationLockService.clearStaleLock(connection);
            }

            // Acquire lock
            lockedBy = migrationLockService.acquireLock(connection, connectionId);
            migrationLockService.updateHeartbeat(connection);

            // Already executed?
            Map<String, Boolean> executedVersions =
                    helper.getScriptExecutedVersions(connectionId, connectionContext.getCurrentDatabase());
            if (Boolean.TRUE.equals(executedVersions.get(targetVersion))) {
                return new MigrationResult(
                        "Migration " + targetVersion + " is already executed successfully.",
                        0,
                        0
                );
            }

            // Find requested migration
            MigrationScript targetScript = loader.loadSpecificVersion(targetVersion,connectionId);
            if(targetScript == null){
                System.out.println("the target script is null");
            };

            // Detect operation
            request.setOperation(
                    sqlOperationDetector.detectOperation(targetScript.getDescription())
            );

            // Execute migration
            engine.migrateUp(
                    targetScript,
                    connectionId,
                    connectionContext.getCurrentDatabase()
            );

            connection.commit();

            return new MigrationResult(
                    "Migration " + targetScript.getVersion() + " executed successfully.",
                    1,
                    0
            );

        } catch (Exception ex) {

            connection.rollback();

            try {
                MigrationScript targetScript = loader.loadPendingMigrations(
                                helper.getExecutedVersions(connectionId, connectionContext.getCurrentDatabase()),
                                connectionId
                        ).stream()
                        .filter(s -> s.getVersion().equals(targetVersion))
                        .findFirst()
                        .orElse(null);

                if (targetScript != null) {
                    migrationRepository.saveFailure(targetScript, connectionId, ex);
                }
            } catch (Exception ignored) {
            }

            return new MigrationResult(
                    "Migration failed: " + ex.getMessage(),
                    0,
                    1
            );

        } finally {

            try {
                migrationLockService.releaseLock(connection, connectionId, lockedBy);
            } catch (Exception ignored) {
            }

            connection.close();
        }
    }

    /**
     * Migration Scripts are Updated : partially Done.
     */
    @Transactional
    public MigrationResult migrateUpdatedScript(MigrationRequest request, String version) throws IOException {
        log.info("Migrating updated script version {} for connection {}", version, request.getConnectionId());
        long connectionId = request.getConnectionId();
        MigrationScript newSCript = loader.loadSpecificVersion(version, connectionId);
        System.out.println(newSCript.getDescription());
        try {
            List<MigrationScript> scripts = loader.loadAllRelatedScript(newSCript, connectionId);

            for (MigrationScript sc : scripts) {
                System.out.println(sc);
            }

            MigrationScript oldScript = loader.getActualScript(scripts.get(0), connectionId);
            log.debug("Loaded previous migration script {}", oldScript);
            String sql = schemaDiffGenerator.generateDiff(oldScript.getUpScript(), newSCript.getUpScript());
            log.debug("Generated alter script: {}", sql);
            Connection conn = connectionService.activeConnection(connectionContext.getCurrentDatabase());
            PreparedStatement statement = conn.prepareStatement(sql);
            int affectedRows = statement.executeUpdate();
            LocalDateTime now = LocalDateTime.now();
            log.info("Updated migration executed successfully. Affected rows: {}", affectedRows);
            helper.saveMigrationRecord(newSCript, connectionId, now.getSecond(), false, connectionService.activeConnection(connectionContext.getCurrentDatabase()));
        } catch (Exception e) {
            log.error("Failed to migrate updated script version {}", version, e);
        }
        return new MigrationResult("✓ Migration complete\nApplied: %s\nSuccess: %d, Failed: %d", 0, 0);
    }

    /**
     * Rollback Migration Scripts : Partially Done.
     */
    @Transactional
    public String rollback(String targetVersion, Long connectionId) {

        try {

            String database = connectionContext.getCurrentDatabase();
            log.info("Starting rollback for database {} and connection {}", database, connectionId);
            List<Migration> history = helper.getMigrationHistory(connectionId, database);
            log.debug("Migration history: {}", history);
            List<MigrationScript> allScripts = history.stream().map(migration -> {
                try {
                    return loader.loadSpecificVersion(migration.getVersion(), connectionId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).filter(Objects::nonNull).toList();
            log.debug("Loaded rollback scripts: {}", allScripts);
            List<MigrationScript> createScripts = allScripts.stream().filter(script -> script.getUpScript().trim().toUpperCase().contains("CREATE TABLE")).toList();
            log.debug("Create scripts considered for rollback: {}", createScripts);
            for (MigrationScript createScript : createScripts) {

                String tableName = helper.extractTableName(createScript.getUpScript());

                log.debug("Processing rollback table {}", tableName);

                List<MigrationScript> dependentScripts = allScripts.stream().filter(script -> script != createScript).filter(script -> {

                    String table = helper.extractTableName(script.getUpScript());

                    return table != null && table.equalsIgnoreCase(tableName);
                }).toList();

                // rollback dependents first
                for (MigrationScript dependent : dependentScripts) {

                    log.info("Rolling back dependent migration {}", dependent.getVersion());

                    boolean success = engine.migrateDown(dependent, database);

                    if (!success) {
                        return "Failed rollback of " + dependent.getVersion();
                    }
                }

                // rollback create table last
                log.info("Rolling back create migration {}", createScript.getVersion());

                boolean success = engine.migrateDown(createScript, database);

                if (!success) {
                    return "Failed rollback of " + createScript.getVersion();
                }
            }

            return "Rollback completed successfully";

        } catch (Exception e) {

            log.error("Rollback failed for connection {} and target version {}", connectionId, targetVersion, e);
            return "Rollback error : " + e.getMessage();
        }
    }

    /**
     * Find The Dependent Script & Execute them : Done
     */
    @Transactional
    public String repair(long connectionId, String versionId) throws SQLException, IOException {
        Connection conn = connectionService.activeConnection(connectionContext.getCurrentDatabase());
        log.info("Finding failed migration {} for repair", versionId);
        MigrationScript script = migrationRepository.findFailedMigrations(versionId, connectionId);
        log.debug("Failed migration {} loaded for repair", versionId);

        log.info("Repairing migration {}", versionId);
        migrationRepository.markAsRepaired(versionId, script, conn, connectionContext.getCurrentDatabase(), connectionId);

        return "✓ Repaired " + versionId + " failed migrations";
    }

    /**
     * Auto Repair migration scripts : Partially Done.
     */
    @Transactional
    public String autoRepair(long connectionId, String versionId) {

        try {

            MigrationScript script =
                    loader.loadSpecificVersion(versionId, connectionId);

            if (script == null) {
                return "Migration not found.";
            }

            repairEngine.repair(script, connectionService.activeConnection(connectionContext.getCurrentDatabase()));

            validationEngine.validate(script, connectionService.activeConnection(connectionContext.getCurrentDatabase()));

            loader.updateMigrationFile(script);

            return "Repair completed successfully.";

        } catch (ValidationException ex) {

            return "Repair completed with remaining issue : "
                    + ex.getMessage();

        } catch (Exception ex) {

            return "Repair failed : " + ex.getMessage();

        }

    }

    @Transactional
    public List<Migration> history(Long connectionId) throws SQLException {
        List<Migration> migrations = helper.getMigrationHistory(connectionId, connectionContext.getCurrentDatabase());
        // Count of migration history records
        log.debug("Total migration history count: {}", migrations.size());
        String[][] data = new String[migrations.size() + 1][5];
        data[0] = new String[]{"Version", "Description", "Executed At", "Time (ms)", "Status"};

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (int i = 0; i < migrations.size(); i++) {
            Migration m = migrations.get(i);
            data[i + 1] = new String[]{m.getVersion(), m.getDescription(), m.getExecutedAt() != null ? m.getExecutedAt().format(formatter) : "-", m.getExecutionTime() != null ? m.getExecutionTime().toString() : "-", m.isSuccess() ? "✓" : "✗"};
        }
        return migrations;
    }

    /**
     * Validate the Migration Script : Done
     */
    @Transactional
    public String validate(Long connectionId, String versionId) {

        log.info("Validating migration {} for connection {}", versionId, connectionId);

        try {

            MigrationScript script = loader.loadSpecificVersion(versionId, connectionId);
            validationEngine.validate(script, connectionService.activeConnection(connectionContext.getCurrentDatabase()));
            return "Validation Successful.";

        } catch (ValidationException ex) {

            log.warn("Validation failed: {}", ex.getMessage());

            return ex.getMessage();

        } catch (Exception ex) {

            log.error("Unexpected validation error.", ex);

            return "Validation Failed : " + ex.getMessage();

        }
    }

    @Transactional
    public StatusResponse status(Long connectionId) throws SQLException {

        if (connectionId == null) {
            throw new RuntimeException("No active connection selected");
        }

//        var currentOpt = helper.getCurrentVersion(connectionId, connectionRequest.getDatabase());
//        String current = currentOpt.orElse("None");
        Set<String> executedVersions = helper.getExecutedVersions(connectionId, connectionContext.getCurrentDatabase());
        try {
//            List<MigrationScript> pending =
//                    loader.loadPendingMigrations(currentOpt.orElse(null), connectionId);
            List<MigrationScript> pending = loader.loadPendingMigrations(executedVersions, connectionId);
            return new StatusResponse(executedVersions.toString(), pending.size(), pending);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load migrations", e);
        }
    }

    @Transactional
    public String rollbackByVersion(String version, String rollbackType, Long connectionId) {
        try {

            String database = connectionContext.getCurrentDatabase();

            log.info("Rollback requested. Version={}, Type={}, Connection={}",
                    version, rollbackType, connectionId);

            MigrationScript scriptOpt =
                    loader.loadSpecificVersion(version, connectionId);

            if (scriptOpt == null) {
                return "Migration " + version + " not found.";
            }

//            MigrationScript script = scriptOpt.get();

            switch (rollbackType.toUpperCase()) {

                case "CREATE":

                    log.info("Rolling back CREATE migration {}", version);

                    if (!engine.migrateDown(scriptOpt, database)) {
                        return "Rollback failed for " + version;
                    }

                    break;

                case "INSERT":

                    log.info("Rolling back INSERT migration {}", version);

                    if (!engine.migrateDown(scriptOpt, database)) {
                        return "Rollback failed for " + version;
                    }

                    break;

                case "ROLLBACK":

                    log.info("Rolling back until version {}", version);

                    List<Migration> history =
                            helper.getMigrationHistory(connectionId, database);

                    history.sort(Comparator.comparing(Migration::getVersion).reversed());

                    for (Migration migration : history) {

                        if (migration.getVersion().equals(version)) {
                            break;
                        }

                        Optional<MigrationScript> migrationScript =
                                Optional.ofNullable(loader.loadSpecificVersion(migration.getVersion(), connectionId));

                        if (migrationScript.isPresent()) {

                            boolean success =
                                    engine.migrateDown(migrationScript.get(), database);

                            if (!success) {
                                return "Failed to rollback " + migration.getVersion();
                            }
                        }
                    }

                    break;

                default:
                    return "Invalid rollback type : " + rollbackType;
            }

            return "Rollback completed successfully.";

        } catch (Exception e) {

            log.error("Rollback failed", e);
            return "Rollback error : " + e.getMessage();
        }
    }
}

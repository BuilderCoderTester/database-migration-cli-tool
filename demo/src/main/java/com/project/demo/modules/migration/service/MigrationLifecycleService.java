package com.project.demo.modules.migration.service;

import com.project.demo.BeforeExecutionValidation.builder.DatabaseSchemaBuilder;
import com.project.demo.BeforeExecutionValidation.comparator.SchemaComparator;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.parser.ASTSchemaExtractor;
import com.project.demo.component.*;
import com.project.demo.infrastructure.exception.MigrationExecutionException;
import com.project.demo.infrastructure.exception.MigrationLoadException;
import com.project.demo.infrastructure.exception.MigrationValidationException;
import com.project.demo.modules.migration.dto.migration.request.ExecuteMigrationRequestDto;
import com.project.demo.modules.migration.dto.migration.request.MigrationRollbackRequestDto;
import com.project.demo.modules.migration.dto.migration.request.MigrationUpdateRequestDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationRepairResponseDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationResultResponseDto;
import com.project.demo.modules.migration.dto.StatusResponse;
import com.project.demo.modules.migration.dto.migration.response.MigrationRollbackResponseDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationUpdateResponseDto;
import com.project.demo.modules.migration.dto.request.MigrationRequest;
import com.project.demo.modules.migration.dto.response.MigrationDescriptionResponse;
import com.project.demo.modules.migration.mappingProfile.MigrationMapper;
import com.project.demo.migrationRepair.engine.RepairEngine;
import com.project.demo.migrationValidator.exception.ValidationException;
import com.project.demo.migrationValidator.validationEngine.ValidationEngine;
import com.project.demo.modules.migration.model.Migration;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.modules.migration.repository.MigrationRepository;
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
     * Execute the Migration Script : Done.
     */
    @Transactional
    public MigrationResultResponseDto executeMigrationScripts(long connectionId) {

        log.info("Starting migration for connection {}", connectionId);

        String database = connectionContext.getCurrentDatabase();
        String lockedBy = null;

        try (Connection connection = connectionService.activeConnection(database)) {

            connection.setAutoCommit(false);

            try {

                // Check stale lock
                if (migrationLockService.isLockStale(connection)) {
                    log.warn("Stale migration lock detected for connection {}", connectionId);
                    migrationLockService.clearStaleLock(connection);
                }

                // Acquire lock
                lockedBy = migrationLockService.acquireLock(connection, connectionId);
                migrationLockService.updateHeartbeat(connection);

                // Load pending migrations
                Set<String> executedVersions =
                        helper.getExecutedVersions(connectionId, database);

                List<MigrationScript> pending =
                        loader.loadPendingMigrations(executedVersions, connectionId);

                if (pending.isEmpty()) {
                    connection.commit();
                    return new MigrationResultResponseDto(
                            "✓ No pending migrations",
                            0,
                            0
                    );
                }

                int success = 0;
                List<String> applied = new ArrayList<>();

                for (MigrationScript script : pending) {

                    if (helper.compareVersion(script.getVersion(), null) > 0) {
                        break;
                    }

                    log.debug("Executing migration {}", script.getVersion());

                    try {

                        engine.migrateUp(
                                script,
                                connectionId,
                                database
                        );

                        success++;
                        applied.add(script.getVersion());

                    } catch (Exception ex) {

                        migrationRepository.saveFailure(script, connectionId, ex);

                        connection.rollback();

                        throw new MigrationExecutionException(
                                "Migration failed at version " + script.getVersion(),
                                ex
                        );
                    }
                }

                connection.commit();

                return new MigrationResultResponseDto(
                        String.format(
                                "✓ Migration completed successfully%nApplied: %s",
                                applied
                        ),
                        success,
                        0
                );

            } catch (IOException ex) {

                connection.rollback();

                throw new MigrationLoadException(
                        "Failed to load migration scripts.",
                        ex
                );

            } catch (SQLException ex) {

                connection.rollback();

                throw new MigrationExecutionException(
                        "Database error while executing migrations.",
                        ex
                );

            } finally {

                try {
                    migrationLockService.releaseLock(
                            connection,
                            connectionId,
                            lockedBy
                    );
                } catch (Exception ex) {
                    log.warn("Failed to release migration lock", ex);
                }
            }

        } catch (SQLException ex) {

            throw new MigrationExecutionException(
                    "Unable to establish database connection.",
                    ex
            );
        }
    }

    /**
     * Execute the Migration Script by Version : Partially Done.
     */
    @Transactional
    public MigrationResultResponseDto executeMigrationScriptsByVersion(
            ExecuteMigrationRequestDto request) throws SQLException {

        Long connectionId = request.connectionId();
        String targetVersion = request.Version();

        if (targetVersion == null || targetVersion.isBlank()) {
            throw new MigrationValidationException("Target version is required.");
        }

        String database = connectionContext.getCurrentDatabase();
        String lockedBy = null;
        MigrationScript targetScript = null;

        Connection connection = connectionService.activeConnection(database);
        connection.setAutoCommit(false);

        try {

            // Remove stale lock
            if (migrationLockService.isLockStale(connection)) {
                migrationLockService.clearStaleLock(connection);
            }

            // Acquire lock
            lockedBy = migrationLockService.acquireLock(connection, connectionId);
            migrationLockService.updateHeartbeat(connection);

            // Current database schema
            SchemaModel currentSchema = databaseSchemaBuilder.build(connection);

            // Executed migrations
            Map<String, Boolean> executedVersions =
                    helper.getScriptExecutedVersions(connectionId, database);

            if (Boolean.TRUE.equals(executedVersions.get(targetVersion))) {

                connection.commit();

                return new MigrationResultResponseDto(
                        "Migration " + targetVersion + " is already executed successfully.",
                        0,
                        0
                );
            }

            // Load migration
            targetScript = loader.loadSpecificVersion(targetVersion, connectionId);

            if (targetScript == null) {
                throw new MigrationValidationException(
                        "Migration " + targetVersion + " not found."
                );
            }

            // Compare schema
            SchemaModel incomingSchema =
                    astSchemaExtractor.extract(targetScript.getUpScript());

            if (schemaComparator.isDuplicate(currentSchema, incomingSchema)) {
                throw new MigrationValidationException(
                        "Migration cannot be executed because the schema already exists."
                );
            }

            // Detect operation

            sqlOperationDetector.detectOperation(
                    targetScript.getDescription()
            );

            // Execute migration
            engine.migrateUp(
                    targetScript,
                    connectionId,
                    database
            );

            connection.commit();

            return new MigrationResultResponseDto(
                    "Migration " + targetScript.getVersion() + " executed successfully.",
                    1,
                    0
            );

        } catch (IOException ex) {

            connection.rollback();

            throw new MigrationLoadException(
                    "Failed to load migration " + targetVersion + ".",
                    ex
            );

        } catch (MigrationValidationException ex) {

            connection.rollback();
            throw ex;

        } catch (Exception ex) {

            connection.rollback();

            if (targetScript != null) {
                try {
                    migrationRepository.saveFailure(
                            targetScript,
                            connectionId,
                            ex
                    );
                } catch (Exception saveEx) {
                    log.warn("Unable to save migration failure.", saveEx);
                }
            }

            throw new MigrationExecutionException(
                    "Migration execution failed.",
                    ex
            );

        } finally {

            try {
                migrationLockService.releaseLock(
                        connection,
                        connectionId,
                        lockedBy
                );
            } catch (Exception ex) {
                log.warn("Unable to release migration lock.", ex);
            }

            try {
                connection.close();
            } catch (Exception ex) {
                log.warn("Unable to close database connection.", ex);
            }
        }
    }

    /**
     * Migration Scripts are Updated : partially Done.
     */
    @Transactional
    public MigrationUpdateResponseDto updateMigrationScriptByVersion(MigrationUpdateRequestDto migrationUpdateRequestDto) throws IOException {
        long connectionId = migrationUpdateRequestDto.connectionId();
        MigrationScript newScript = loader.loadSpecificVersion(migrationUpdateRequestDto.versionId(), connectionId);
        System.out.println(newScript.getDescription());
        try {
            List<MigrationScript> scripts = loader.loadAllRelatedScript(newScript, connectionId);

            for (MigrationScript sc : scripts) {
                System.out.println(sc);
            }

            MigrationScript oldScript = loader.getActualScript(scripts.get(0), connectionId);
            log.debug("Loaded previous migration script {}", oldScript);
            String sql = schemaDiffGenerator.generateDiff(oldScript.getUpScript(), newScript.getUpScript());
            log.debug("Generated alter script: {}", sql);
            Connection conn = connectionService.activeConnection(connectionContext.getCurrentDatabase());
            PreparedStatement statement = conn.prepareStatement(sql);
            int affectedRows = statement.executeUpdate();
            LocalDateTime now = LocalDateTime.now();
            log.info("Updated migration executed successfully. Affected rows: {}", affectedRows);
            helper.saveMigrationRecord(newScript, connectionId, now.getSecond(), false, connectionService.activeConnection(connectionContext.getCurrentDatabase()));
        } catch (Exception e) {
            log.error("Failed to migrate updated script version {}", migrationUpdateRequestDto.versionId(), e);
        }
        return new MigrationUpdateResponseDto("✓ Migration complete\nApplied: %s\nSuccess: %d, Failed: %d", migrationUpdateRequestDto.versionId(), "Update", true);
    }

    /**
     * Rollback Migration Scripts : Partially Done.
     */
    @Transactional
    public MigrationRollbackResponseDto rollbackMigrationScript(MigrationRollbackRequestDto requestDto) {

        try {

            String database = connectionContext.getCurrentDatabase();
            log.info("Starting rollback for database {} and connection {}", database, requestDto.connectionId());
            List<Migration> history = helper.getMigrationHistory(requestDto.connectionId(), database);
            log.debug("Migration history: {}", history);
            List<MigrationScript> allScripts = history.stream().map(migration -> {
                try {
                    return loader.loadSpecificVersion(migration.getVersion(), requestDto.connectionId());
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
                        return new MigrationRollbackResponseDto( "Failed rollback of ",dependent.getVersion(),"Create",false);
                    }
                }

                // rollback create table last
                log.info("Rolling back create migration {}", createScript.getVersion());

                boolean success = engine.migrateDown(createScript, database);

                if (!success) {
                    return new MigrationRollbackResponseDto( "Failed rollback of ",createScript.getVersion(),"Create",false);
                }
            }

            return new MigrationRollbackResponseDto("Migration Scrip is rollback", requestDto.targetVersion(), "Rollback", true);

        } catch (Exception e) {

            log.error("Rollback failed for connection {} and target version {}", requestDto.connectionId(), requestDto.targetVersion(), e);
            return new MigrationRollbackResponseDto("Migration Scrip is rollback", requestDto.targetVersion(), "Rollback", true);
        }
    }

    /**
     * Find The Dependent Script & Execute them : Done
     */
    @Transactional
    public MigrationRepairResponseDto repair(long connectionId, String versionId) throws SQLException, IOException {
        Connection conn = connectionService.activeConnection(connectionContext.getCurrentDatabase());
        log.info("Finding failed migration {} for repair", versionId);
        MigrationScript script = migrationRepository.findFailedMigrations(versionId, connectionId);
        log.debug("Failed migration {} loaded for repair", versionId);

        log.info("Repairing migration {}", versionId);
        migrationRepository.markAsRepaired(versionId, script, conn, connectionContext.getCurrentDatabase(), connectionId);

        return new MigrationRepairResponseDto("✓ Repaired " + versionId + " failed migrations");
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
    public List<MigrationDescriptionResponse> history(Long connectionId) throws SQLException {

        List<Migration> migrations =
                helper.getMigrationHistory(connectionId, connectionContext.getCurrentDatabase());

        List<MigrationDescriptionResponse> responses =
                MigrationMapper.mapMigrationToDescriptive(migrations);

        log.debug("Total migration history count: {}", responses.size());

        return responses;
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
        Set<String> executedVersions = helper.getExecutedVersions(connectionId, connectionContext.getCurrentDatabase());
        try {
            List<MigrationScript> pending = loader.loadPendingMigrations(executedVersions, connectionId);
            return new StatusResponse(executedVersions.toString(), pending.size(), pending);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load migrations", e);
        }
    }

    @Transactional
    public MigrationRollbackResponseDto rollbackMigrationScriptByVersion(
            MigrationRollbackRequestDto requestDto) {

        String database = connectionContext.getCurrentDatabase();

        log.info(
                "Rollback requested. Version={}, Type={}, Connection={}",
                requestDto.targetVersion(),
                requestDto.rollbackType(),
                requestDto.connectionId());

        if (requestDto.connectionId() == null) {
            throw new MigrationValidationException("Connection Id is required.");
        }

        if (requestDto.targetVersion() == null || requestDto.targetVersion().isBlank()) {
            throw new MigrationValidationException("Target version is required.");
        }

        if (requestDto.rollbackType() == null || requestDto.rollbackType().isBlank()) {
            throw new MigrationValidationException("Rollback type is required.");
        }

        try {

            MigrationScript targetScript =
                    loader.loadSpecificVersion(
                            requestDto.targetVersion(),
                            requestDto.connectionId());

            if (targetScript == null) {
                throw new MigrationValidationException(
                        "Migration " + requestDto.targetVersion() + " not found.");
            }

            switch (requestDto.rollbackType().toUpperCase()) {

                case "CREATE":
                case "INSERT":

                    rollbackSingleMigration(targetScript, database);

                    break;

                case "ROLLBACK":

                    rollbackUntilVersion(requestDto, database);

                    break;

                default:
                    throw new MigrationValidationException(
                            "Unsupported rollback type : " + requestDto.rollbackType());
            }

            log.info("Rollback completed successfully.");

            return new MigrationRollbackResponseDto(
                    "Rollback completed successfully.",
                    requestDto.targetVersion(),
                    requestDto.rollbackType(),
                    true);

        } catch (IOException ex) {

            throw new MigrationLoadException(
                    "Unable to load migration script.",
                    ex);

        } catch (MigrationValidationException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new MigrationExecutionException(
                    "Rollback failed.",
                    ex);
        }
    }

    private void rollbackSingleMigration(
            MigrationScript script,
            String database) {

        boolean success = engine.migrateDown(script, database);

        if (!success) {
            throw new MigrationExecutionException(
                    "Rollback failed for migration " + script.getVersion());
        }
    }

    private void rollbackUntilVersion(
            MigrationRollbackRequestDto requestDto,
            String database) throws IOException, SQLException {

        List<Migration> history =
                helper.getMigrationHistory(
                        requestDto.connectionId(),
                        database);

        history.sort(
                Comparator.comparing(Migration::getVersion).reversed());

        for (Migration migration : history) {

            if (migration.getVersion().equals(requestDto.targetVersion())) {
                break;
            }

            MigrationScript script =
                    loader.loadSpecificVersion(
                            migration.getVersion(),
                            requestDto.connectionId());

            if (script == null) {
                continue;
            }

            boolean success = engine.migrateDown(script, database);

            if (!success) {
                throw new MigrationExecutionException(
                        "Rollback failed for migration " + migration.getVersion());
            }
        }
    }
}

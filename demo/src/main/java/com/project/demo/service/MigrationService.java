package com.project.demo.service;

import com.project.demo.core.MigrationEngine;
import com.project.demo.core.MigrationLoader;
import com.project.demo.dto.*;
import com.project.demo.model.ConnectionConfig;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.ConnectionRepo;
import com.project.demo.repository.MigrationRepository;
import com.project.demo.utility.Helper;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MigrationService {

    private final Helper helper;
    private final MigrationLoader loader;
    private final MigrationEngine engine;
    private final MigrationLockService migrationLockService;
    private final MigrationRepository repository;
    private ConnectionRequest connectionRequest;
    private ConnectionRepo connectionRepo;
    public MigrationService(Helper helper, MigrationLoader loader, MigrationEngine engine, MigrationLockService migrationLockService, MigrationRepository repository, ConnectionRepo connectionRepo) {
        this.helper = helper;
        this.loader = loader;
        this.engine = engine;
        this.migrationLockService = migrationLockService;
        this.repository = repository;
        this.connectionRepo = connectionRepo;
    }

    public StatusResponse status (){
        var currentOpt = helper.getCurrentVersion();
        String current = currentOpt.orElse("None");

        try {
            List<MigrationScript> pending = loader.loadPendingMigrations(currentOpt.orElse(null));

            StringBuilder sb = new StringBuilder();
            sb.append("Current Version: ").append(current).append("\n");
            sb.append("Pending Migrations: ").append(pending.size()).append("\n\n");

            StatusResponse statusResponse = new StatusResponse(currentOpt.toString(),pending.size(),pending);

            if (!pending.isEmpty()) {
                sb.append("Pending:\n");
                for (MigrationScript script : pending) {
                    sb.append("  - V").append(script.getVersion())
                            .append(": ").append(script.getDescription()).append("\n");
                }
            }

            return statusResponse;
        } catch (IOException e) {
//            return "Error loading migrations: " + e.getMessage();
                return new StatusResponse();
        }
    }

    public void initialize() {
        engine.initialize();
    }

    public List<MigrationScript> listAllPendingMigration() {
        return loader.listAllPendingMigration();
    }

    public MigrationResult migrate(String targetVersion) {
        try {
            migrationLockService.acquireLock();
            System.out.println("MIGRATION LOCK IS ACQUIRED! ");
            var currentOpt = helper.getCurrentVersion();
            System.out.println("CURRENT VERSION : "+ currentOpt);
            List<MigrationScript> pending = loader.loadPendingMigrations(currentOpt.orElse(null));
            for(MigrationScript script : pending){
                System.out.println("PENDING : " + script);
            }
            StringBuilder message = new StringBuilder();
//            MigrationResult result = new MigrationResult();

            if (pending.isEmpty()) {
                message.append("✓ No pending migrations");
            }

            int success = 0;
            int failed = 0;
            List<String> applied = new ArrayList<>();

            for (MigrationScript script : pending) {

                if (targetVersion != null &&
                        helper.compareVersion(script.getVersion(), targetVersion) > 0) {
                    break;
                }
                // MIGRATION START HERE
                try {
                    engine.migrateUp(script);
                    success++;
                    applied.add(script.getVersion());

                } catch (Exception e) {
                    failed++;

                    message.append(String.format(
                            "✗ Migration failed at version %s\nReason: %s\nApplied: %s",
                            script.getVersion(),
                            e.getMessage(),
                            applied
                    ));
                    return new MigrationResult(message.toString(),success,failed);
                }
            }

            return new MigrationResult( String.format(
                    "✓ Migration complete\nApplied: %s\nSuccess: %d, Failed: %d",
                    applied,
                    success,
                    failed
            ),success,failed);

        } catch (IOException e) {
            return new MigrationResult("✗ Migration failed: " + e.getMessage(),0,0);
        }
        finally {
            try {
                migrationLockService.releaseLock();
            }catch (Exception e){
                // ignore
            }
        }
    }

    public String rollback(@Option(description = "Target version") String targetVersion) {
        try {
            var currentOpt = helper.getCurrentVersion();
            if (currentOpt.isEmpty()) {
                return "No migrations to rollback";
            }

            String current = currentOpt.get();
            MigrationScript script = loader.loadSpecificVersion(current);

            if (script == null) {
                return "Could not find migration script for version: " + current;
            }

            if (engine.migrateDown(script)) {
                return "✓ Rolled back version " + current;
            } else {
                return "✗ Rollback failed";
            }

        } catch (IOException e) {
            return "Rollback error: " + e.getMessage();
        }
    }

    public String repair() {

        List<Migration> failed = repository.findFailedMigrations();

        if (failed.isEmpty()) {
            return "✓ No failed migrations";
        }

        for (Migration m : failed) {

            // 🔧 mark as not dirty
            repository.clearDirtyFlag(m.getVersion());

            // optional: mark as repaired
            repository.markAsRepaired(m.getVersion());
        }

        return "✓ Repaired " + failed.size() + " failed migrations";
    }

    public List<Migration> history() {
        List<Migration> migrations = helper.getMigrationHistory();

        String[][] data = new String[migrations.size() + 1][5];
        data[0] = new String[]{"Version", "Description", "Executed At", "Time (ms)", "Status"};

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (int i = 0; i < migrations.size(); i++) {
            Migration m = migrations.get(i);
            data[i + 1] = new String[]{
                    m.getVersion(),
                    m.getDescription(),
                    m.getExecutedAt() != null ? m.getExecutedAt().format(formatter) : "-",
                    m.getExecutionTime() != null ? m.getExecutionTime().toString() : "-",
                    m.isSuccess() ? "✓" : "✗"
            };
        }

//        return new TableBuilder(new ArrayTableModel(data))
//                .addFullBorder(BorderStyle.fancy_light)
//                .build();
        return migrations;
    }

    public String create(
            @Option(required = true, description = "Migration version") String version,
            @Option(required = true, description = "Description") String description,
            @Option(defaultValue = "", description = "Up SQL") String up,
            @Option(defaultValue = "", description = "Down SQL") String down
    ) {
        try {
            loader.createMigrationFile(version, description, up, down);
            return String.format("✓ Created migration V%s__%s.sql", version, description.replace(" ", "_"));
        } catch (IOException e) {
            return "Failed to create migration: " + e.getMessage();
        }
    }

    public String validate() {
        try {
            List<Migration> history = helper.getMigrationHistory();
            int errors = 0;

            for (Migration migration : history) {
                MigrationScript script = loader.loadSpecificVersion(migration.getVersion());
                if (script != null && !helper.validateChecksum(migration.getVersion(), script.getUpScript())) {
                    errors++;
                    System.out.println("Checksum mismatch: " + migration.getVersion());
                }
            }

            return errors == 0 ? "✓ All migrations validated" : "✗ " + errors + " checksum errors found";
        } catch (IOException e) {
            return "Validation error: " + e.getMessage();
        }
    }

    public ConnectionResponse connect(ConnectionRequest connectionRequest) {

        String baseUrl = "jdbc:postgresql://"
                + connectionRequest.getHost() + ":"
                + connectionRequest.getPort() + "/postgres";

        try {
            // 1. Connect to default DB
            DataSource ds = DataSourceBuilder.create()
                    .url(baseUrl)
                    .username(connectionRequest.getUsername())
                    .password(connectionRequest.getPassword())
                    .build();

            Connection conn = ds.getConnection();

            // 2. Create DB if not exists
            String dbName = connectionRequest.getDatabase();

            if (!dbName.matches("[a-zA-Z0-9_]+")) {
                throw new RuntimeException("Invalid database name");
            }

            String sql = "CREATE DATABASE " + dbName;

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Database created: " + dbName);
            } catch (SQLException e) {
                if (!e.getMessage().contains("already exists")) {
                    throw e;
                }
            }

            conn.close();

            // 3. Test connection to new DB
            String newDbUrl = "jdbc:postgresql://"
                    + connectionRequest.getHost() + ":"
                    + connectionRequest.getPort() + "/"
                    + dbName;

            DataSource newDs = DataSourceBuilder.create()
                    .url(newDbUrl)
                    .username(connectionRequest.getUsername())
                    .password(connectionRequest.getPassword())
                    .build();

            newDs.getConnection().close();

            // 🔥 4. SAVE CONNECTION (THIS IS WHAT YOU WERE MISSING)
            ConnectionConfig config = new ConnectionConfig();
            config.setName(connectionRequest.getName());
            config.setHost(connectionRequest.getHost());
            config.setPort(connectionRequest.getPort());
            config.setDatabase(connectionRequest.getDatabase());
            config.setUsername(connectionRequest.getUsername());
            config.setPassword(connectionRequest.getPassword());
            config.setSchema(connectionRequest.getSchema());

            ConnectionConfig saved = connectionRepo.save(config);

            // 🔥 5. RETURN ID
            return new ConnectionResponse(true, "Connection successful", saved.getConnectionId());

        } catch (SQLException e) {
            return new ConnectionResponse(false, e.getMessage(), null);
        }
    }
}

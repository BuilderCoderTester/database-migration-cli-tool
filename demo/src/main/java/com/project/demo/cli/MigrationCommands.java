// MigrationCommands.java
package com.project.demo.cli;

import com.project.demo.core.MigrationEngine;
import com.project.demo.core.MigrationLoader;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.MigrationRepository;
import com.project.demo.service.MigrationLockService;
import com.project.demo.utility.Helper;
import lombok.AllArgsConstructor;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.table.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Command(group = "Database Migration")
@AllArgsConstructor
public class MigrationCommands {

    private final MigrationEngine engine;
    private final MigrationLoader loader;
    private final Helper helper;
    private final MigrationLockService migrationLockService;
    private final MigrationRepository repository;


    @Command(command = "init", description = "Initialize migration schema")
    public String initialize() {
        engine.initialize();
        return "✓ Migration schema initialized successfully";
    }

    @Command(command = "status", description = "Show current migration status")
    public String status() {
        var currentOpt = helper.getCurrentVersion();
        String current = currentOpt.orElse("None");

        try {
            List<MigrationScript> pending = loader.loadPendingMigrations(currentOpt.orElse(null));

            StringBuilder sb = new StringBuilder();
            sb.append("Current Version: ").append(current).append("\n");
            sb.append("Pending Migrations: ").append(pending.size()).append("\n\n");

            if (!pending.isEmpty()) {
                sb.append("Pending:\n");
                for (MigrationScript script : pending) {
                    sb.append("  - V").append(script.getVersion())
                            .append(": ").append(script.getDescription()).append("\n");
                }
            }

            return sb.toString();
        } catch (IOException e) {
            return "Error loading migrations: " + e.getMessage();
        }
    }

    @Command(command = "migrate", description = "Run pending migrations")
    public String migrate(@Option(description = "Target version") String targetVersion) {

        try {
            migrationLockService.acquireLock();
            var currentOpt = helper.getCurrentVersion();
            List<MigrationScript> pending = loader.loadPendingMigrations(currentOpt.orElse(null));

            if (pending.isEmpty()) {
                return "✓ No pending migrations";
            }

            int success = 0;
            int failed = 0;
            List<String> applied = new ArrayList<>();

            for (MigrationScript script : pending) {

                if (targetVersion != null &&
                        helper.compareVersion(script.getVersion(), targetVersion) > 0) {
                    break;
                }

                try {
                    engine.migrateUp(script);
                    success++;
                    applied.add(script.getVersion());

                } catch (Exception e) {
                    failed++;

                    return String.format(
                            "✗ Migration failed at version %s\nReason: %s\nApplied: %s",
                            script.getVersion(),
                            e.getMessage(),
                            applied
                    );
                }
            }

            return String.format(
                    "✓ Migration complete\nApplied: %s\nSuccess: %d, Failed: %d",
                    applied,
                    success,
                    failed
            );

        } catch (IOException e) {
            return "✗ Migration failed: " + e.getMessage();
        }
        finally {
            try {
                migrationLockService.releaseLock();
            }catch (Exception e){
                // ignore
            }
        }
    }



    @Command(command = "rollback", description = "Rollback last migration or to specific version")
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

    @Command(command = "repair", description = "Repair failed migrations")
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

    @Command(command = "history", description = "Show migration history")
    public Table history() {
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

        return new TableBuilder(new ArrayTableModel(data))
                .addFullBorder(BorderStyle.fancy_light)
                .build();
    }

    @Command(command = "create", description = "Create new migration file")
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

    @Command(command = "validate", description = "Validate migrations against database")
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
}
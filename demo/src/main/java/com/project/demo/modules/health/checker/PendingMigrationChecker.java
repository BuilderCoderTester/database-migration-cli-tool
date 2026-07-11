package com.project.demo.modules.health.checker;

import com.project.demo.modules.health.dto.HealthCheckResult;
import com.project.demo.modules.health.interfaces.HealthChecker;
import com.project.demo.modules.health.model.HealthStatus;
import com.project.demo.modules.migration.model.ConnectionConfig;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.modules.migration.service.MigrationService;

import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

public class PendingMigrationChecker implements HealthChecker {

    private final MigrationService migrationService;

    public PendingMigrationChecker(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public HealthCheckResult check(Connection connection,
                                   ConnectionConfig config) {

        long start = System.currentTimeMillis();

        try {

            List<MigrationScript> pending =
                    migrationService.getPendingMigrations(connection, config);

            if (pending.isEmpty()) {

                return new HealthCheckResult(
                        "Pending Migrations",
                        HealthStatus.HEALTHY,
                        "No pending migrations.",
                        System.currentTimeMillis() - start
                );
            }

            String versions = pending.stream()
                    .map(MigrationScript::getVersion)
                    .collect(Collectors.joining(", "));

            return new HealthCheckResult(
                    "Pending Migrations",
                    HealthStatus.WARNING,
                    pending.size() + " pending migration(s): " + versions,
                    System.currentTimeMillis() - start
            );

        } catch (Exception ex) {

            return new HealthCheckResult(
                    "Pending Migrations",
                    HealthStatus.CRITICAL,
                    ex.getMessage(),
                    System.currentTimeMillis() - start
            );

        }

    }

}
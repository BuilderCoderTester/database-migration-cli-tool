package com.project.demo.modules.health.checker;

import com.project.demo.modules.health.dto.HealthCheckResult;
import com.project.demo.modules.health.interfaces.HealthChecker;
import com.project.demo.modules.health.model.HealthStatus;
import com.project.demo.modules.migration.model.ConnectionConfig;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.modules.migration.repository.MigrationRepository;

import java.sql.Connection;
import java.util.List;

public class FailedMigrationChecker implements HealthChecker {

    private final MigrationRepository migrationRepository;

    public FailedMigrationChecker(MigrationRepository migrationRepository) {
        this.migrationRepository = migrationRepository;
    }

    @Override
    public HealthCheckResult check(Connection connection,
                                   ConnectionConfig config) {

        long start = System.currentTimeMillis();

        try {

            List<MigrationScript> failedMigrations =
                    migrationRepository.getFailedMigrations(connection);

            if (failedMigrations.isEmpty()) {

                return new HealthCheckResult(
                        "Failed Migrations",
                        HealthStatus.HEALTHY,
                        "No failed migrations found.",
                        System.currentTimeMillis() - start
                );

            }

            StringBuilder message = new StringBuilder();

            message.append(failedMigrations.size())
                    .append(" failed migration(s): ");

            for (MigrationScript migration : failedMigrations) {

                message.append(migration.getVersion())
                        .append(", ");

            }

            message.setLength(message.length() - 2);

            return new HealthCheckResult(
                    "Failed Migrations",
                    HealthStatus.CRITICAL,
                    message.toString(),
                    System.currentTimeMillis() - start
            );

        } catch (Exception ex) {

            return new HealthCheckResult(
                    "Failed Migrations",
                    HealthStatus.CRITICAL,
                    ex.getMessage(),
                    System.currentTimeMillis() - start
            );

        }

    }

}
package com.project.demo.modules.health.checker;

import com.project.demo.modules.health.dto.HealthCheckResult;
import com.project.demo.modules.health.interfaces.HealthChecker;
import com.project.demo.modules.health.model.HealthStatus;
import com.project.demo.modules.migration.model.ConnectionConfig;
import com.project.demo.modules.migration.service.MigrationLockService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;

public class MigrationLockChecker implements HealthChecker {

    @Autowired
    private  MigrationLockService migrationLockService;

    public MigrationLockChecker(MigrationLockService migrationLockService) {
        this.migrationLockService = migrationLockService;
    }

    @Override
    public HealthCheckResult check(Connection connection,
                                   ConnectionConfig config) {

        long start = System.currentTimeMillis();

        try {

            if (!migrationLockService.isLockStale(connection)) {

                return new HealthCheckResult(
                        "Migration Lock",
                        HealthStatus.HEALTHY,
                        "No active migration lock found.",
                        System.currentTimeMillis() - start
                );
            }

            if (migrationLockService.isLockStale(connection)) {

                return new HealthCheckResult(
                        "Migration Lock",
                        HealthStatus.CRITICAL,
                        "A stale migration lock was detected. Manual intervention may be required.",
                        System.currentTimeMillis() - start
                );
            }

            return new HealthCheckResult(
                    "Migration Lock",
                    HealthStatus.WARNING,
                    "A migration is currently running.",
                    System.currentTimeMillis() - start
            );

        } catch (Exception ex) {

            return new HealthCheckResult(
                    "Migration Lock",
                    HealthStatus.CRITICAL,
                    ex.getMessage(),
                    System.currentTimeMillis() - start
            );
        }
    }
}
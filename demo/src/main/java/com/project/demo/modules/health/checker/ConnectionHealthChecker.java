package com.project.demo.modules.health.checker;

import com.project.demo.modules.health.dto.HealthCheckResult;
import com.project.demo.modules.health.interfaces.HealthChecker;
import com.project.demo.modules.health.model.HealthStatus;
import com.project.demo.modules.migration.model.ConnectionConfig;

import java.sql.Connection;

public class ConnectionHealthChecker implements HealthChecker {

    @Override
    public HealthCheckResult check(Connection connection,
                                   ConnectionConfig config) {

        long start = System.currentTimeMillis();

        try {

            if (connection == null) {

                return new HealthCheckResult(
                        "Database Connection",
                        HealthStatus.CRITICAL,
                        "Connection object is null.",
                        System.currentTimeMillis() - start
                );
            }

            if (connection.isClosed()) {

                return new HealthCheckResult(
                        "Database Connection",
                        HealthStatus.CRITICAL,
                        "Connection is closed.",
                        System.currentTimeMillis() - start
                );
            }

            if (!connection.isValid(5)) {

                return new HealthCheckResult(
                        "Database Connection",
                        HealthStatus.CRITICAL,
                        "Database connection is invalid.",
                        System.currentTimeMillis() - start
                );
            }

            return new HealthCheckResult(
                    "Database Connection",
                    HealthStatus.HEALTHY,
                    "Successfully connected to database.",
                    System.currentTimeMillis() - start
            );

        } catch (Exception ex) {

            return new HealthCheckResult(
                    "Database Connection",
                    HealthStatus.CRITICAL,
                    ex.getMessage(),
                    System.currentTimeMillis() - start
            );

        }

    }

}
package com.project.demo.modules.health.checker;

import com.project.demo.modules.health.dto.HealthCheckResult;
import com.project.demo.modules.health.interfaces.HealthChecker;
import com.project.demo.modules.health.model.HealthStatus;
import com.project.demo.modules.migration.model.ConnectionConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class PermissionHealthChecker implements HealthChecker {

    @Override
    public HealthCheckResult check(Connection connection,
                                   ConnectionConfig config) {

        long start = System.currentTimeMillis();

        try {

            DatabaseMetaData metaData = connection.getMetaData();

            if (!metaData.supportsTransactions()) {

                return new HealthCheckResult(
                        "Database Capabilities",
                        HealthStatus.CRITICAL,
                        "Database does not support transactions.",
                        System.currentTimeMillis() - start
                );
            }

            if (!metaData.supportsBatchUpdates()) {

                return new HealthCheckResult(
                        "Database Capabilities",
                        HealthStatus.WARNING,
                        "Database does not support batch updates.",
                        System.currentTimeMillis() - start
                );
            }

            return new HealthCheckResult(
                    "Database Capabilities",
                    HealthStatus.HEALTHY,
                    "Database supports required migration features.",
                    System.currentTimeMillis() - start
            );

        } catch (Exception ex) {

            return new HealthCheckResult(
                    "Database Capabilities",
                    HealthStatus.CRITICAL,
                    ex.getMessage(),
                    System.currentTimeMillis() - start
            );

        }

    }

}
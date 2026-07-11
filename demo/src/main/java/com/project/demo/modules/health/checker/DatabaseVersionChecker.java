package com.project.demo.modules.health.checker;

import com.project.demo.modules.health.dto.HealthCheckResult;
import com.project.demo.modules.health.interfaces.HealthChecker;
import com.project.demo.modules.health.model.HealthStatus;
import com.project.demo.modules.migration.model.ConnectionConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class DatabaseVersionChecker implements HealthChecker {

    @Override
    public HealthCheckResult check(Connection connection,
                                   ConnectionConfig config) {

        long start = System.currentTimeMillis();

        try {

            DatabaseMetaData metaData = connection.getMetaData();

            String databaseName = metaData.getDatabaseProductName();
            String version = metaData.getDatabaseProductVersion();

            String message =
                    databaseName + " " + version;

            return new HealthCheckResult(
                    "Database Version",
                    HealthStatus.HEALTHY,
                    message,
                    System.currentTimeMillis() - start
            );

        } catch (Exception ex) {

            return new HealthCheckResult(
                    "Database Version",
                    HealthStatus.CRITICAL,
                    ex.getMessage(),
                    System.currentTimeMillis() - start
            );
        }
    }

}
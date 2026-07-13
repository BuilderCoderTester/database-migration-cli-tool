package com.project.demo.modules.health.checker;

import com.project.demo.modules.health.dto.HealthCheckResult;
import com.project.demo.modules.health.interfaces.HealthChecker;
import com.project.demo.modules.health.model.HealthStatus;
import com.project.demo.modules.migration.model.ConnectionConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseStatusChecker implements HealthChecker {

    @Override
    public HealthCheckResult check(Connection connection,
                                   ConnectionConfig config) {

        long start = System.currentTimeMillis();

        try {

            DatabaseMetaData metaData = connection.getMetaData();

            String databaseProduct =
                    metaData.getDatabaseProductName();

            String validationQuery =
                    getValidationQuery(databaseProduct);

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(validationQuery)) {

                if (rs.next()) {

                    return new HealthCheckResult(
                            "Database Status",
                            HealthStatus.HEALTHY,
                            databaseProduct + " is online.",
                            System.currentTimeMillis() - start
                    );
                }

            }

            return new HealthCheckResult(
                    "Database Status",
                    HealthStatus.WARNING,
                    "Database responded but returned no result.",
                    System.currentTimeMillis() - start
            );

        } catch (Exception ex) {

            return new HealthCheckResult(
                    "Database Status",
                    HealthStatus.CRITICAL,
                    ex.getMessage(),
                    System.currentTimeMillis() - start
            );

        }

    }

    private String getValidationQuery(String databaseProduct) {

        switch (databaseProduct.toLowerCase()) {

            case "postgresql":
                return "SELECT current_database();";

            case "mysql":
                return "SELECT DATABASE();";

            case "microsoft sql server":
                return "SELECT DB_NAME();";

            case "oracle":
                return "SELECT name FROM v$database";

            default:
                return "SELECT 1";
        }
    }

}
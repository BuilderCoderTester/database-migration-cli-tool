package com.project.demo.modules.health.service;

import com.project.demo.modules.health.dto.HealthCheckResult;
import com.project.demo.modules.health.model.HealthStatus;
import com.project.demo.modules.health.interfaces.HealthChecker;
import com.project.demo.modules.health.model.HealthReport;
import com.project.demo.modules.migration.model.ConnectionConfig;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@Service
public class HealthCheckService {
    private final List<HealthChecker> checkers;

    public HealthCheckService(List<HealthChecker> checkers) {
        this.checkers = checkers;
    }

    public HealthReport check(Connection connection,
                              ConnectionConfig config) {

        List<HealthCheckResult> results = new ArrayList<>();

        for (HealthChecker checker : checkers) {

            try {

                results.add(
                        checker.check(connection, config)
                );

            } catch (Exception ex) {

                results.add(
                        new HealthCheckResult(
                                checker.getClass().getSimpleName(),
                                HealthStatus.CRITICAL,
                                ex.getMessage(),
                                0
                        )
                );
            }
        }

        return buildReport(results);

    }
}

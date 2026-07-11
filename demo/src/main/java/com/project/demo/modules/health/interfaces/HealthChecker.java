package com.project.demo.modules.health.interfaces;

import com.project.demo.modules.health.dto.HealthCheckResult;
import com.project.demo.modules.migration.model.ConnectionConfig;

import java.sql.Connection;

public interface HealthChecker {
    HealthCheckResult check(Connection connection,
                            ConnectionConfig config) throws Exception;

}

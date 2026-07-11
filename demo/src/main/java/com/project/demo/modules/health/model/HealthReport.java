package com.project.demo.modules.health.model;

import com.project.demo.modules.health.dto.HealthCheckResult;

import java.util.List;

public class HealthReport {
    private List<HealthCheckResult> checks;

    private HealthStatus overallStatus;
}

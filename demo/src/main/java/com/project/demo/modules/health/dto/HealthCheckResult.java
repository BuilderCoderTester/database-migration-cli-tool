package com.project.demo.modules.health.dto;

import com.project.demo.modules.health.model.HealthStatus;

public record HealthCheckResult(String checkName,

                                HealthStatus status,

                                String message,

                                long executionTime
) {
}

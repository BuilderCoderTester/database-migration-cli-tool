package com.project.demo.modules.health.model;

import com.project.demo.modules.health.dto.HealthCheckResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class HealthReport {
    private List<HealthCheckResult> checks;

    private HealthStatus overallStatus;
}

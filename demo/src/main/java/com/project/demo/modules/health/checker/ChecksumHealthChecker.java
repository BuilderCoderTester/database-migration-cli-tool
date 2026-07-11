package com.project.demo.modules.health.checker;

import com.project.demo.modules.health.dto.ChecksumMismatch;
import com.project.demo.modules.health.dto.HealthCheckResult;
import com.project.demo.modules.health.interfaces.HealthChecker;
import com.project.demo.modules.health.model.HealthStatus;
import com.project.demo.modules.migration.model.ConnectionConfig;
import com.project.demo.modules.migration.service.ChecksumService;

import java.sql.Connection;
import java.util.List;

public class ChecksumHealthChecker implements HealthChecker {

    private final ChecksumService checksumService;

    public ChecksumHealthChecker(ChecksumService checksumService) {
        this.checksumService = checksumService;
    }

    @Override
    public HealthCheckResult check(Connection connection,
                                   ConnectionConfig config) {

        long start = System.currentTimeMillis();

        try {

            List<ChecksumMismatch> mismatches =
                    checksumService.validateChecksums(connection, config);

            if (mismatches.isEmpty()) {

                return new HealthCheckResult(
                        "Checksum Validation",
                        HealthStatus.HEALTHY,
                        "All migration checksums are valid.",
                        System.currentTimeMillis() - start
                );

            }

            StringBuilder builder = new StringBuilder();

            builder.append(mismatches.size())
                    .append(" checksum mismatch(es): ");

            for (ChecksumMismatch mismatch : mismatches) {

                builder.append(mismatch.getVersion())
                        .append(", ");

            }

            builder.setLength(builder.length() - 2);

            return new HealthCheckResult(
                    "Checksum Validation",
                    HealthStatus.CRITICAL,
                    builder.toString(),
                    System.currentTimeMillis() - start
            );

        } catch (Exception ex) {

            return new HealthCheckResult(
                    "Checksum Validation",
                    HealthStatus.CRITICAL,
                    ex.getMessage(),
                    System.currentTimeMillis() - start
            );

        }

    }

}
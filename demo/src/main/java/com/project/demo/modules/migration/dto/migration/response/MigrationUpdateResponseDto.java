package com.project.demo.modules.migration.dto.migration.response;

public record MigrationUpdateResponseDto(
        String message,
        String version,
        String operation,
        boolean updated
) {
}

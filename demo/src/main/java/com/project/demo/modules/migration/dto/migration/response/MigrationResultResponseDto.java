package com.project.demo.modules.migration.dto.migration.response;

public record MigrationResultResponseDto(
         String message,
         int successCount,
         int failedCount
) {

}

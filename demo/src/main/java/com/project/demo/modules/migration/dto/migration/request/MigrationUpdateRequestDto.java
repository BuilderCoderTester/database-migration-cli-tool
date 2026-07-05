package com.project.demo.modules.migration.dto.migration.request;

public record MigrationUpdateRequestDto(
        long connectionId,
        String versionId
) {
}

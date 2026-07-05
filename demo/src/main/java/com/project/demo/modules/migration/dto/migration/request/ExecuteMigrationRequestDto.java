package com.project.demo.modules.migration.dto.migration.request;

public record ExecuteMigrationRequestDto(
        long connectionId,
        String Version
) {
}

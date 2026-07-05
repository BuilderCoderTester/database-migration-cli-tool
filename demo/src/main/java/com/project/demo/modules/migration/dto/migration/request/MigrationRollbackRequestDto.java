package com.project.demo.modules.migration.dto.migration.request;

public record MigrationRollbackRequestDto(String targetVersion,
                                          Long connectionId,
                                          String rollbackType) {
}

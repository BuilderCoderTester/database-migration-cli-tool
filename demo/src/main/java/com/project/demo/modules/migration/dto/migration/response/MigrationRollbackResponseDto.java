package com.project.demo.modules.migration.dto.migration.response;

public record MigrationRollbackResponseDto (String message,
                                            String version,
                                            String operation,
                                            boolean rollback){
}

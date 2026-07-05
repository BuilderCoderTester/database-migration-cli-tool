package com.project.demo.modules.migration.mappingProfile;

import com.project.demo.modules.migration.dto.logs.response.MigrationLogsResponseDto;
import com.project.demo.modules.migration.model.MigrationLogs;

import java.util.List;

public class LogMapper {
    public static MigrationLogsResponseDto logToResponse(MigrationLogs migrationLogs) {
        if (migrationLogs == null) {
            return null;
        }

        return new MigrationLogsResponseDto(
                migrationLogs.getId(),
                migrationLogs.getMessage(),
                migrationLogs.getLevel(),
                migrationLogs.getTimestamp()
        );
    }

    public static List<MigrationLogsResponseDto> logToResponse(List<MigrationLogs> logs) {
        return logs.stream()
                .map(LogMapper::logToResponse)
                .toList();
    }
}

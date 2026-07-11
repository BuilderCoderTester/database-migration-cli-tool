package com.project.demo.modules.migration.dto.logs.response;

import com.project.demo.enumuration.LogLevel;

import java.time.LocalDateTime;

public record MigrationLogsResponseDto(
        int id,

        String message,

        LogLevel level,

        LocalDateTime timestamp
) {

}

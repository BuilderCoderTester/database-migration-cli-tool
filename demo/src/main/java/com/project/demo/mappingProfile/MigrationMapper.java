package com.project.demo.mappingProfile;

import com.project.demo.dto.MigrationDetailsDTO;
import com.project.demo.dto.MigrationDetailsResponse;
import com.project.demo.dto.MigrationStatisticsDTO;
import com.project.demo.dto.RelatedScriptDTO;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MigrationMapper {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static MigrationDetailsDTO toDto(Migration migration) {

        return MigrationDetailsDTO.builder()
                .version(migration.getVersion())
                .description(migration.getDescription())
                .type(migration.isRepeatable() ? "REPEATABLE" : "VERSIONED")
                .status(migration.isSuccess() ? "SUCCESS" : "FAILED")
                .success(migration.isSuccess())
                .author(null)
                .database(null)
                .connectionName(
                        migration.getConnection() != null
                                ? migration.getConnection().getName()
                                : null)
                .executedAt(
                        migration.getExecutedAt() != null
                                ? migration.getExecutedAt().format(FORMATTER)
                                : null)
                .executionTime(migration.getExecutionTime())
                .checksum(migration.getChecksum())
                .fileName(migration.getName())
                .filePath(null)
                .script(migration.getScript())
                .rollbackScript(null)
                .rollbackAvailable(false)
                .validation(null)
                .build();
    }

    public static RelatedScriptDTO toRelatedScriptDTO(MigrationScript script) {

        return new RelatedScriptDTO(
                script.getVersion(),
                script.getDescription(),
                script.isRepeatable() ? "REPEATABLE" : "VERSIONED",
                "PENDING",     // Set appropriately if you know the status
                false          // Set appropriately if you know whether it succeeded
        );
    }

    public static List<RelatedScriptDTO> map(List<MigrationScript> scripts) {
        return scripts.stream()
                .map(MigrationMapper::toRelatedScriptDTO)
                .toList();
    }

    public static MigrationDetailsResponse toResponse(
            MigrationDetailsDTO migration,
            List<RelatedScriptDTO> relatedScripts,
            MigrationStatisticsDTO statistics) {

        return new MigrationDetailsResponse(
                migration,
                relatedScripts,
                statistics
        );
    }
}

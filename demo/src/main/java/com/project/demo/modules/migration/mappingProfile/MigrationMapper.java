package com.project.demo.modules.migration.mappingProfile;

import com.project.demo.modules.migration.dto.MigrationDetailsDTO;
import com.project.demo.modules.migration.dto.MigrationDetailsResponse;
import com.project.demo.modules.migration.dto.MigrationStatisticsDTO;
import com.project.demo.modules.migration.dto.RelatedScriptDTO;
import com.project.demo.modules.migration.dto.response.MigrationDescriptionResponse;
import com.project.demo.modules.migration.dto.response.MigrationScriptCreateResponse;
import com.project.demo.modules.migration.dto.response.ValidationResult;
import com.project.demo.modules.migration.model.Migration;
import com.project.demo.modules.migration.model.MigrationScript;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MigrationMapper {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static MigrationDetailsDTO toDto(Migration migration) {
        if (migration == null) {
            return null;
        }
        return MigrationDetailsDTO.builder()
                .version(migration.getVersion())
                .description(migration.getDescription())
                .type(migration.isRepeatable() ? "REPEATABLE" : "VERSIONED")
                .status(migration.getSuccess() ? "SUCCESS" : "FAILED")
                .success(migration.getSuccess())
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
                .executionTime(migration.getExecutionTime() != null ? migration.getExecutionTime() : 0L)
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

    public static MigrationScriptCreateResponse toResponse(
            ValidationResult result) {

        if (result.isValid()) {
            return MigrationScriptCreateResponse.success(
                    result.getMessage(),
                    null,
                    result.getTableName()
            );
        }

        return MigrationScriptCreateResponse.failure(
                result.getMessage(),
                buildReason(result)
        );
    }

    private static String buildReason(ValidationResult result) {

        if (result.getExistingMigrationVersion() != null) {
            return String.format(
                    "Table '%s' already exists in migration %s (%s).",
                    result.getTableName(),
                    result.getExistingMigrationVersion(),
                    result.getExistingMigrationName()
            );
        }

        return result.getMessage();
    }

    public static List<MigrationDescriptionResponse> mapMigrationToDescriptive(List<Migration> migration){
        return migration.stream()
                .map(MigrationMapper::mapMigrationToDesc)
                .toList();
    }
    public static MigrationDescriptionResponse mapMigrationToDesc(Migration migration){
        MigrationDescriptionResponse migrationDescriptionResponse = new MigrationDescriptionResponse();
        migrationDescriptionResponse.setDescription(migration.getDescription());
        migrationDescriptionResponse.setExecutedAt(migration.getExecutedAt());
        migrationDescriptionResponse.setExecutionTime(migration.getExecutionTime());
        migrationDescriptionResponse.setScript(migration.getScript());
        migrationDescriptionResponse.setSuccess(migration.getSuccess());
        migrationDescriptionResponse.setVersion(migration.getVersion());
        return  migrationDescriptionResponse;
    }
}

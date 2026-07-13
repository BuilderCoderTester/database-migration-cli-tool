package com.project.demo.modules.migration.service;

import com.project.demo.component.ConnectionContext;
import com.project.demo.component.MigrationLoader;
import com.project.demo.modules.migration.dto.MigrationDetailsDTO;
import com.project.demo.modules.migration.dto.MigrationDetailsResponse;
import com.project.demo.modules.migration.dto.MigrationStatisticsDTO;
import com.project.demo.modules.migration.dto.RelatedScriptDTO;
import com.project.demo.modules.migration.dto.response.MigrationScriptCreateResponse;
import com.project.demo.modules.migration.dto.response.ValidationResult;
import com.project.demo.infrastructure.exception.MigrationCreationException;
import com.project.demo.modules.migration.mappingProfile.MigrationMapper;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.modules.migration.repository.MigrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Service
@Slf4j
public class MigrationScriptService {

    @Autowired
    private MigrationLoader loader;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ConnectionContext connectionContext;
    @Autowired
    private MigrationRepository migrationRepository;

    public MigrationScriptCreateResponse create(
             String version,
             String description,
             String up,
             String down
    ) {
        try {
            Connection connection = connectionService.activeConnection(connectionContext.getCurrentDatabase());
            ValidationResult result = loader.createMigrationFile(description, up, down, connection);
            return MigrationMapper.toResponse(result);

        } catch (Exception e) {
            throw new MigrationCreationException(
                    "Failed to create migration.",
                    e
            );
        }
    }

    public String update(String version, String upSql, String downSql, long connectionId) throws IOException {
        MigrationScript migrationScript = loader.loadSpecificVersion(version, connectionId);

        int versionNumber = Integer.parseInt(version.replace("V", ""));
        log.debug("Parsed migration version number {}", versionNumber);

        String tableName = migrationScript.getDescription()
                .substring(migrationScript.getDescription().lastIndexOf(' ') + 1);

        log.debug("Detected table name {} for migration update", tableName);

        String updatedDescription = "Update table " + tableName;

        create(version, updatedDescription, upSql, downSql);

        return String.format("✓ Updated migration V%s__%s.sql",
                version,
                migrationScript.getDescription());
    }

    public MigrationScript viewScript(String version, long coonnectionId) throws IOException {
        log.debug("Loading migration script {} for connection {}", version, coonnectionId);
        MigrationScript script = loader.loadSpecificVersion(version, coonnectionId);
        return script;
    }

    public void delete(long connectionId, String versionId) throws SQLException {
        String sql = "DELETE FROM sub_migration WHERE version = ? AND connection_id = ?";

        try (Connection connection = connectionService.activeConnection(connectionContext.getCurrentDatabase());
             PreparedStatement statement = connection.prepareStatement(sql)) {
            log.debug("Deleting migration {} for connection {}", versionId, connectionId);
            statement.setString(1, versionId);
            statement.setLong(2, connectionId);

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected == 0) {
                throw new RuntimeException("Migration not found for version: " + versionId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getLatestMigrationVersion() {
        return loader.getLatestVersion();
    }

    public MigrationDetailsResponse getMigrationDetails(Long connectionId, String versionId) throws IOException, SQLException {
        System.out.println("the version " + versionId);
        Connection connection = connectionService.activeConnection(connectionContext.getCurrentDatabase());

        MigrationScript originalScript = loader.loadSpecificVersion(versionId, connectionId);
        System.out.println("the script is " + originalScript);
        MigrationDetailsDTO actualScript =
                migrationRepository.loadMigrationScriptDetails(versionId, connectionId, connection);

        actualScript.setAuthor("localhost");
        actualScript.setDatabase(connectionContext.getCurrentDatabase());
        actualScript.setConnectionName(connection.toString());
        actualScript.setFilePath(loader.getFilePath(connectionId, versionId));
        actualScript.setRollbackScript(loader.loadSpecificVersion(versionId, connectionId).getDownScript());

        List<RelatedScriptDTO> relatedScripts = MigrationMapper.map(loader.loadAllRelatedScript(originalScript, connectionId));

        MigrationStatisticsDTO migrationStatisticsDTO = loader.calculateStatistics(relatedScripts);

        return MigrationMapper.toResponse(actualScript, relatedScripts, migrationStatisticsDTO);

    }

}

package com.project.demo.service;

import com.project.demo.component.ConnectionContext;
import com.project.demo.component.MigrationLoader;
import com.project.demo.dto.MigrationDetailsDTO;
import com.project.demo.dto.MigrationDetailsResponse;
import com.project.demo.dto.MigrationStatisticsDTO;
import com.project.demo.dto.RelatedScriptDTO;
import com.project.demo.mappingProfile.MigrationMapper;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.MigrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public String create(
            @Option(required = false, description = "Migration version") String version,
            @Option(required = true, description = "Description") String description,
            @Option(defaultValue = "", description = "Up SQL") String up,
            @Option(defaultValue = "", description = "Down SQL") String down
    ) {
        try {
            loader.createMigrationFile(description, up, down);
            return String.format("✓ Created migration V%s__%s.sql", version, description.replace(" ", "_"));
        } catch (IOException e) {
            return "Failed to create migration: " + e.getMessage();
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
        }
    }

    public int getLatestMigrationVersion() {
        return loader.getLatestVersion();
    }

    public MigrationDetailsResponse getMigrationDetails(Long connectionId, String versionId) throws IOException, SQLException {
        Connection connection = connectionService.activeConnection(connectionContext.getCurrentDatabase());

        MigrationScript originalScript = loader.loadSpecificVersion(versionId, connectionId);

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

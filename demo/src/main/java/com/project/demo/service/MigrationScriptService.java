package com.project.demo.service;

import com.project.demo.component.ConnectionContext;
import com.project.demo.component.MigrationLoader;
import com.project.demo.model.MigrationScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class MigrationScriptService {

    @Autowired
    private MigrationLoader loader;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ConnectionContext connectionContext;

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

        int versionNumber = Integer.parseInt(version.replace("V",""));
        System.out.println("the version number is " + versionNumber);

        String tableName = migrationScript.getDescription()
                .substring(migrationScript.getDescription().lastIndexOf(' ') + 1);

        System.out.println("table name " + tableName);

        String updatedDescription = "Update table " + tableName;

        create(version, updatedDescription, upSql, downSql);

        return String.format("✓ Updated migration V%s__%s.sql",
                version,
                migrationScript.getDescription());
    }

    public MigrationScript viewScript(String version , long coonnectionId) throws IOException {
        System.out.println("reach point -1 here script ");
        MigrationScript script = loader.loadSpecificVersion(version ,coonnectionId);
        return  script;
    }

    public void delete(long connectionId, String versionId) throws SQLException {
        String sql = "DELETE FROM sub_migration WHERE version = ? AND connection_id = ?";

        try (Connection connection =connectionService.activeConnection(connectionContext.getCurrentDatabase());
             PreparedStatement statement = connection.prepareStatement(sql)) {
            System.out.println("Version = " + versionId);
            System.out.println("Connection Id = " + connectionId);
            statement.setString(1, versionId);
            statement.setLong(2, connectionId);

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected == 0) {
                throw new RuntimeException("Migration not found for version: " + versionId);
            }
        }
    }

    public int getLatestMigrationVersion(){
        return loader.getLatestVersion();
    }

}

package com.project.demo.component;

import com.project.demo.BeforeExecutionValidation.SchemaValidatorService;
import com.project.demo.config.MigrationProperties;
import com.project.demo.modules.migration.dto.MigrationStatisticsDTO;
import com.project.demo.modules.migration.dto.RelatedScriptDTO;
import com.project.demo.modules.migration.dto.response.ValidationResult;
import com.project.demo.enumuration.LogLevel;
import com.project.demo.modules.migration.model.Migration;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.modules.migration.repository.MigrationLogRepo;
import com.project.demo.modules.migration.repository.MigrationRepository;
import com.project.demo.modules.migration.service.LogService;
import com.project.demo.modules.migration.service.MigrationValidatorService;
import com.project.demo.utility.Helper;
import com.project.demo.utility.VersionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.*;
import java.util.Comparator;

@Component
@Slf4j
public class MigrationLoader {

    private static final Pattern MIGRATION_PATTERN =
            Pattern.compile("V(\\d+)__(.+)\\.sql");

    private static final Pattern VERSIONED_PATTERN =
            Pattern.compile("V(\\d+)__([\\w_]+)\\.sql");

    private static final Pattern REPEATABLE_PATTERN =
            Pattern.compile("R__([\\w_]+)\\.sql");

    @Autowired
    private MigrationProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final LogService logService;
    private final Helper helper;
    @Autowired
    private SchemaValidatorService schemaValidatorService;
    @Autowired
    private MigrationValidatorService migrationValidatorService;
    @Autowired
    private MigrationRepository migrationRepository;

    public MigrationLoader(JdbcTemplate jdbcTemplate, MigrationLogRepo migrationLogRepo, LogService logService, Helper helper) {
        this.jdbcTemplate = jdbcTemplate;
        this.logService = logService;
        this.helper = helper;
    }

    // LOAD PENDING MIGRATION ON SPECIFIC DATABASE CONNECTION
    public List<MigrationScript> loadPendingMigrations(Set<String> executedVersions, Long connectionId) throws IOException {

        if (connectionId == null) {
            throw new RuntimeException("No active connection selected");
        }

        List<MigrationScript> versioned = new ArrayList<>();
        List<MigrationScript> repeatables = new ArrayList<>();

        Path basePath = Paths.get(properties.getPath());
        Path path = basePath.resolve("conn_" + connectionId);

        if (!Files.exists(path)) {
            Files.createDirectories(path);
            return List.of();
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.sql")) {

            for (Path file : stream) {

                String fileName = file.getFileName().toString();
                Matcher v = VERSIONED_PATTERN.matcher(fileName);
                Matcher r = REPEATABLE_PATTERN.matcher(fileName);

                String content = Files.readString(file); // ✅ FIXED
                // ========================
                // VERSIONED MIGRATIONS
                // ========================
                if (v.matches()) {

                    String version = "V" + v.group(1);
                    String description = v.group(2).replace("_", " ");

//                    if (currentVersion == null ||
//                            VersionUtils.extract(version) > VersionUtils.extract(currentVersion)) {
//
//                        MigrationScript script = parseScript(version, description, content);
//                        script.setFileName(fileName);
//                        script.setRepeatable(false);
//
//                        versioned.add(script);
//                    }

                    executedVersions =
                            helper.getExecutedVersions(
                                    connectionId,
                                    connectionContext.getCurrentDatabase()
                            );
                    if (!executedVersions.contains(version)) {

                        MigrationScript script =
                                parseScript(version, description, content);

                        script.setFileName(fileName);
                        script.setRepeatable(false);

                        versioned.add(script);
                    }

                }

                // ========================
                // REPEATABLE MIGRATIONS
                // ========================
                else if (r.matches()) {

                    String name = r.group(1);

                    MigrationScript script = parseScript("R__" + name, name, content);
                    script.setFileName(fileName);
                    script.setRepeatable(true);
                    script.setName(name);

                    repeatables.add(script);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // 🔥 sort versioned (numeric safe)
        versioned.sort(Comparator.comparingLong(
                m -> VersionUtils.extract(m.getVersion())
        ));

        // 🔥 sort repeatables
        repeatables.sort(Comparator.comparing(MigrationScript::getName));

        // 🔥 combine
        List<MigrationScript> result = new ArrayList<>();
        result.addAll(versioned);
        result.addAll(repeatables);

        return result;
    }

    @Autowired
    public ConnectionContext connectionContext;

    public MigrationScript loadSpecificVersion(String version, Long connectionId) throws IOException {
        Path path = Paths.get(properties.getPath());
        Path connectionPath = path.resolve("conn_" + connectionId);
        String exactPattern = String.format("%s__*.sql", version);
        log.debug("Loading migration {} from {}", exactPattern, connectionPath);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(connectionPath, exactPattern)) {
            for (Path file : stream) {
                String content = Files.readString(file);
                String desc = file.getFileName().toString()
                        .replaceFirst("V\\d+__", "")
                        .replace(".sql", "");
//                        .replace("_", " ");
                return parseScript(version, desc, content);
            }
        }
        return null;
    }

    public int getLatestVersion() {
        Long connectionId = connectionContext.getCurrentConnectionId();
        if (connectionId == null) {
            throw new RuntimeException("No active connection. Please connect first.");
        }

        Path basePath = Paths.get(properties.getPath());
        Path connectionFolder = basePath.resolve("conn_" + connectionId);

        int latestVersion = 0;

        if (Files.exists(connectionFolder)) {
            try (Stream<Path> files = Files.list(connectionFolder)) {
                OptionalInt maxVersion = files
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql"))
                        .map(p -> {
                            Matcher m = Pattern.compile("V(\\d+)__").matcher(p.getFileName().toString());
                            return m.find() ? Integer.parseInt(m.group(1)) : 0;
                        })
                        .mapToInt(Integer::intValue)
                        .max();

                if (maxVersion.isPresent()) {
                    latestVersion = maxVersion.getAsInt();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read migrations folder", e);
            }
        }

        return latestVersion + 1;
    }

    private MigrationScript parseScript(String version, String description, String content) {
        String upScript = content;
        String downScript = null;
        // Support for -- DOWN marker to separate up/down scripts
        int downIndex = content.indexOf("-- DOWN");
        if (downIndex != -1) {
            upScript = content.substring(0, downIndex).trim();
            downScript = content.substring(downIndex + 7).trim();
        }

        return new MigrationScript(version, description, upScript, downScript);
    }

    /// CREATION OF MIGRATION FILE
    /// PARAMETERS - VERSION , DESCRIPTION , UP_SCRIPT , DOWN_SCRIPT
    public ValidationResult createMigrationFile(String description, String upScript, String downScript, Connection connection)
            throws Exception {

        Long connectionId = connectionContext.getCurrentConnectionId();
        System.out.println("teh connection id : "+ connectionId);
        if (connectionId == null) {
            logService.log("No active connection. Please connect first.", LogLevel.ERROR);
            throw new RuntimeException("No active connection. Please connect first.");
        }

        Path basePath = Paths.get(properties.getPath());
        Path connectionFolder = basePath.resolve("conn_" + connectionId);
        Files.createDirectories(connectionFolder);

        // ── AUTO-INCREMENT VERSION LOGIC ──
        int nextVersion = 1; // default if no files exist

        try (Stream<Path> files = Files.list(connectionFolder)) {
            List<Path> migrationFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql"))
                    .sorted(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime((Path) p).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).reversed()) // most recent first
                    .collect(Collectors.toList());

            if (!migrationFiles.isEmpty()) {
                Path latestFile = migrationFiles.get(0);
                String fileName = latestFile.getFileName().toString();

                // Extract version number: V{number}__...
                Matcher matcher = Pattern.compile("V(\\d+)__").matcher(fileName);
                if (matcher.find()) {
                    nextVersion = Integer.parseInt(matcher.group(1)) + 1;
                }
            }
        }
        // ──────────────────────────────────

        String version = String.valueOf(nextVersion);
        String fileName = String.format("V%s__%s.sql", version, description.replace(" ", "_"));
        Path filePath = connectionFolder.resolve(fileName);

        StringBuilder content = new StringBuilder();
        content.append("-- Migration: ").append(description).append("\n");
        content.append("-- Version: ").append(version).append("\n\n");
        content.append(upScript);

        if (downScript != null && !downScript.isEmpty()) {
            content.append("\n\n-- DOWN\n\n");
            content.append(downScript);
        }
        String properVersion = 'V' + version;
        System.out.println("the version is " + properVersion);
        MigrationScript migrationScript = new MigrationScript();
        migrationScript.setVersion(properVersion);
        migrationScript.setDescription(description);
        migrationScript.setName(fileName);
        migrationScript.setUpScript(upScript);
        migrationScript.setDownScript(downScript);

//        System.out.println("The migration script is : " + migrationScript.toString());
        ValidationResult result = migrationValidatorService.validate(migrationScript, loadOtherMigrationScript(migrationScript.getVersion(), connectionId));
        System.out.println("the result after is : "+result.toString());
        if (result.isValid()) {
            Files.writeString(filePath, content.toString());

            // ---------- SAVE TO SUB_MIGRATION TABLE ----------
            migrationRepository.saveCreatedMigration(
                    migrationScript,
                    connectionId,
                    connection, content
            );

            logService.log((fileName + " Migration Applied."), LogLevel.SUCCESS);
            return ValidationResult.success("Migration Applied !!!!! ", migrationScript.getName());
        } else {

            logService.log((fileName + " Migration Failed."), LogLevel.ERROR);
        }
        return ValidationResult.error("Migration not Applied !HAHA!HADA! ");

    }

    public List<MigrationScript> loadOtherMigrationScript(
            String targetVersion,
            long connectionId) throws IOException {

        Path path = Paths.get(properties.getPath());
        Path connectedPath = path.resolve("conn_" + connectionId);

        if (!Files.exists(connectedPath)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(connectedPath)) {

            return files
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .map(file -> {
                        try {
                            String name = file.getFileName().toString();
                            String[] parts = name.replace(".sql", "").split("__");

                            String content = Files.readString(file);

                            MigrationScript script =
                                    parseScript(parts[0], parts[1], content);

                            script.setFileName(name);

                            return script;

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(script -> !script.getVersion().equalsIgnoreCase(targetVersion))
                    .sorted(Comparator.comparing(MigrationScript::getVersion))
                    .toList();
        }
    }

    public List<MigrationScript> listAllPendingMigration(Long connectionId, Connection connection) throws SQLException {
        List<Migration> pendingScripts =
                migrationRepository.loadAllPendingMigrationScript(connection, connectionId);

        return pendingScripts.stream()
                .map(this::mapToMigrationScript)
                .toList();
    }

    private MigrationScript mapToMigrationScript(Migration migration) {
        MigrationScript script = new MigrationScript();

        script.setVersion(migration.getVersion());
        script.setDescription(migration.getDescription());
        // Map the remaining fields as needed

        return script;
    }
    public List<MigrationScript> loadFromFolder(Long connectionId) throws IOException {

        if (connectionId == null) {
            throw new RuntimeException("No active connection is selected.");
        }
        Path path = Paths.get("migrations");
        Path connectedPath = path.resolve("conn_" + connectionId);
        if (!Files.exists(connectedPath)) {
            return List.of();
        }

        return Files.list(connectedPath)
                .filter(p -> p.getFileName().toString().endsWith(".sql"))
                .map(this::parseFileName)
                .sorted(Comparator.comparing(MigrationScript::getVersion))
                .toList();
    }

    private MigrationScript parseFileName(Path file) {

        String name = file.getFileName().toString(); // V1__init.sql
        System.out.println("the name is" + name);
        String[] parts = name.replace(".sql", "").split("__");

        return new MigrationScript(
                parts[0],  // version
                parts[1]   // description
        );
    }

    private Set<String> loadExecutedVersionsFromDB(
            Long connectionId,
            Connection connection
    ) throws SQLException {

        String sql = """
                SELECT version
                FROM sub_migration
                WHERE success = true
                AND connection_id = ?
                """;

        Set<String> versions = new HashSet<>();

        try (PreparedStatement stmt =
                     connection.prepareStatement(sql)) {

            stmt.setLong(1, connectionId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    String version =
                            rs.getString("version");

                    log.debug("Executed migration version {}", version);

                    versions.add(version);
                }
            }
        }

        return versions;
    }

    // Load all specific related script
    public List<MigrationScript> loadAllRelatedScript(MigrationScript loadScript, long connectionId) throws IOException {

        String tableName = loadScript.getDescription()
                .trim()
                .substring(loadScript.getDescription().lastIndexOf('_') + 1)
                .toLowerCase();
        System.out.println(tableName);
        log.debug("Target table name for related scripts: {}", tableName);

        Path path = Paths.get("migrations");
        Path connectedPath = path.resolve("conn_" + connectionId);

        if (!Files.exists(connectedPath)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(connectedPath)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .map(this::parseFileName)
                    .filter(script -> {
                        String description = script.getDescription();

                        if (description == null) {
                            return false;
                        }

                        String[] parts = description.toLowerCase().split("_");
                        String scriptTableName = parts[parts.length - 1];

                        log.trace("Comparing script table {} with target table {}", scriptTableName, tableName);

                        return scriptTableName.equals(tableName);
                    })
                    .sorted(Comparator.comparing(MigrationScript::getVersion))
                    .toList();
        }
    }

    public MigrationScript getActualScript(MigrationScript migrationScript, long connectionId) throws IOException {
        String version = migrationScript.getVersion();
        return loadSpecificVersion(version, connectionId);

    }

    public void updateMigrationFile(MigrationScript script) throws IOException {

        Long connectionId = connectionContext.getCurrentConnectionId();

        if (connectionId == null) {
            throw new RuntimeException("No active connection.");
        }

        Path basePath = Paths.get(properties.getPath());
        Path connectionFolder = basePath.resolve("conn_" + connectionId);

        Path filePath = connectionFolder.resolve(script.getFileName());

        StringBuilder content = new StringBuilder();

        content.append("-- Migration: ")
                .append(script.getDescription())
                .append("\n");

        content.append("-- Version: ")
                .append(script.getVersion().replace("V", ""))
                .append("\n\n");

        content.append(script.getUpScript());

        if (script.getDownScript() != null &&
                !script.getDownScript().isBlank()) {

            content.append("\n\n-- DOWN\n\n");

            content.append(script.getDownScript());

        }

        Files.writeString(
                filePath,
                content.toString(),
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE
        );

    }

    public String getFilePath(long connectionId, String version) throws IOException {

        Path basePath = Paths.get(properties.getPath());
        Path connectionPath = basePath.resolve("conn_" + connectionId);

        String pattern = String.format("%s__*.sql", version);

        log.debug("Searching migration {} in {}", pattern, connectionPath);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(connectionPath, pattern)) {
            for (Path file : stream) {
                return file.toAbsolutePath().toString();
            }
        }

        return null;
    }

    public MigrationStatisticsDTO calculateStatistics(List<RelatedScriptDTO> scripts) {

        int total = scripts.size();

        int successful = 0;
        int failed = 0;
        int pending = 0;

        for (RelatedScriptDTO script : scripts) {

            switch (script.getStatus().toUpperCase()) {
                case "SUCCESS" -> successful++;
                case "FAILED" -> failed++;
                case "PENDING" -> pending++;
            }
        }

        return new MigrationStatisticsDTO(
                total,
                successful,
                failed,
                pending
        );
    }

}

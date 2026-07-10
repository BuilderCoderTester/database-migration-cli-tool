package com.project.demo.utility;

import com.project.demo.component.*;
import com.project.demo.config.ConfigManager;
import com.project.demo.config.DatabaseConfig;
import com.project.demo.migrationValidator.exception.ValidationException;
import com.project.demo.modules.migration.model.Migration;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.modules.migration.repository.MigrationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
@Component
@Slf4j
public class Helper {

    private final SqlExecutor sqlExecutor;
    private final MigrationRepository repository;
    private final MigrationValidator validator;
    private final JdbcTemplate jdbcTemplate;
    private final ConnectionContext connectionContext;
    /// compare the scripts version
    public int compareVersion(String v1, String v2) {

        long n1 = extractNumericVersion(v1);
        long n2 = extractNumericVersion(v2);

        return Long.compare(n1, n2);
    }

    /// extract the version number from the scripts
    public long extractNumericVersion(String version) {

        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }

        // Remove all non-digits (e.g., "V1" → "1")
        String numeric = version.replaceAll("\\D", "");

        if (numeric.isEmpty()) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        return Long.parseLong(numeric);
    }

    /// version id manageable
    public void applyVersioned(MigrationScript script, long start, Long connectionId) throws SQLException {
        Connection connection = activeConnection(connectionContext.getCurrentDatabase());
        log.info("Applying versioned migration {} to database {}", script.getVersion(), connectionContext.getCurrentDatabase());
        sqlExecutor.executeScript(script.getUpScript(),connection,connectionContext.getCurrentDatabase());
        saveMigrationRecord(script, connectionId,System.currentTimeMillis() - start, false,connection);
    }
    public void updateMigrationStatus(String version, Long connectionId,
                                      com.project.demo.enumuration.Status status, long executionTime) throws SQLException {
        String sql = """
            UPDATE sub_migration
            SET status = ?, execution_time = ?, installed_on = NOW()
            WHERE version = ? AND connection_id = ?
            """;

        try (Connection connection =activeConnection(connectionContext.getCurrentDatabase());
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status.name());
            statement.setLong(2, executionTime);
            statement.setString(3, version);
            statement.setLong(4, connectionId);

            statement.executeUpdate();
        }
    }
    /// SAVE THE MIGRATION RECORDS
    public void saveMigrationRecord(MigrationScript script,Long connectionId,
                                    long executionTime,
                                    boolean repeatable,Connection connection) throws SQLException {

        Migration m = new Migration(
                script.getVersion(),
                script.getDescription(),
                script.getUpScript()
        );

        m.setChecksum(calculateChecksum(script.getUpScript()));
        m.setExecutedAt(LocalDateTime.now());
        m.setExecutionTime(executionTime);
        m.setSuccess(true);
        m.setDirty(false);
        m.setRepeatable(repeatable);
        m.setName(script.getName());

        repository.save(m,connectionId ,connection);
    }

    /// SFA checksum
    public String calculateChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /// get current version of the schema or scripts
    public Optional<String> getCurrentVersion(Long connectionId,String databaseName) throws SQLException {
        Connection connection = activeConnection(databaseName);
        return repository.findLastSuccessful(connectionId,connection).map(Migration::getVersion);
    }

    public Set<String> getExecutedVersions(
            Long connectionId,
            String databaseName
    ) throws SQLException {

        Set<String> versions = new HashSet<>();

        String sql = """
        SELECT version
        FROM sub_migration
        """;

        try (
                Connection conn = activeConnection(connectionContext.getCurrentDatabase());
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {
                versions.add(rs.getString("version"));
            }
        }

        return versions;
    }

    public Map<String, Boolean> getScriptExecutedVersions(
            Long connectionId,
            String databaseName
    ) throws SQLException {

        Map<String, Boolean> versions = new HashMap<>();

        String sql = """
        SELECT version, success
        FROM sub_migration
        """;

        try (
                Connection conn = activeConnection(connectionContext.getCurrentDatabase());
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {
                versions.put(
                        rs.getString("version"),
                        rs.getBoolean("success")
                );
            }
        }

        return versions;
    }
    public Connection activeConnection(String databaseName) throws SQLException {
        String sql = """
                    SELECT connection_id FROM connections WHERE database = ?
                """;
        String url = """
                SELECT url from connections where connection_id = ?
                """;
        Long connection_id = jdbcTemplate.queryForObject(sql, Long.class, databaseName);
        connectionContext.setCurrentConnectionId(connection_id);
        String dbUrl = jdbcTemplate.queryForObject(url,String.class,connection_id);
        try {
            DatabaseConfig databaseConfig = ConfigManager.load();
            Connection newConnection = DriverManager.getConnection(
                    dbUrl,
                    databaseConfig.getUsername(),
                    databaseConfig.getPassword()
            );
            return newConnection;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    /// history of migration records
    public List<Migration> getMigrationHistory(Long connectionId,String currentDatabase) throws SQLException {
        String databaseName = connectionContext.getCurrentDatabase();
        Connection connection = activeConnection(currentDatabase);
        return repository.findAll(connectionId,connection,databaseName);
    }

    ///  checksum validation
    public boolean validateChecksum(String version, String script) throws SQLException {
        Connection connection = activeConnection(connectionContext.getCurrentDatabase());
        Optional<Migration> existing = repository.findByVersion(version,connection);
        if (existing.isEmpty()) return true;

        String currentChecksum = calculateChecksum(script);
        return currentChecksum.equals(existing.get().getChecksum());
    }

    /// database dirty
    public boolean isDatabaseDirty() {
        return validator.validateDirtyDb();
    }

    /// pending migration request
//    public int getPendingCount(Long connectionId) {
//        try {
//            String currentVersion = getCurrentVersion(connectionId).orElse(null);
//            return migrationLoader.loadPendingMigrations(currentVersion,connectionId).size();
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to fetch pending migrations", e);
//        }
//    }

    public String extractTableName(String sql) {

        if (sql == null || sql.isBlank()) {
            return null;
        }

        String normalized =
                sql.trim()
                        .replaceAll("\\s+", " ")
                        .toUpperCase();

        if (normalized.startsWith("CREATE TABLE")) {
            return normalized.split(" ")[2];
        }

        if (normalized.startsWith("INSERT INTO")) {
            return normalized.split(" ")[2];
        }

        if (normalized.startsWith("ALTER TABLE")) {
            return normalized.split(" ")[2];
        }

        if (normalized.startsWith("UPDATE")) {
            return normalized.split(" ")[1];
        }

        if (normalized.startsWith("DELETE FROM")) {
            return normalized.split(" ")[2];
        }

        return null;
    }

    private String extractUpSql(String migrationContent) {

        String upMarker = "-- Write your UP SQL here";
        String downMarker = "-- DOWN";

        int upIndex = migrationContent.indexOf(upMarker);

        if (upIndex == -1) {
            throw new IllegalArgumentException(
                    "UP SQL marker not found");
        }

        int start = upIndex + upMarker.length();

        int downIndex =
                migrationContent.indexOf(downMarker, start);

        if (downIndex == -1) {
            throw new IllegalArgumentException(
                    "DOWN marker not found");
        }

        return migrationContent
                .substring(start, downIndex)
                .trim();
    }

    public List<String> extractIndexFromQuerry(
            String upScript,
            String downScript,
            Connection connection,
            long connectionId) {

        List<String> indexes = new ArrayList<>();

        // CREATE INDEX idx_name ON table(...)
        Pattern createIndexPattern = Pattern.compile(
                "CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(\\w+)",
                Pattern.CASE_INSENSITIVE);

        Matcher createIndexMatcher = createIndexPattern.matcher(upScript);

        while (createIndexMatcher.find()) {
            indexes.add(createIndexMatcher.group(1));
        }

        // ALTER TABLE ... ADD INDEX idx_name(...)
        Pattern alterIndexPattern = Pattern.compile(
                "ADD\\s+INDEX\\s+(\\w+)",
                Pattern.CASE_INSENSITIVE);

        Matcher alterIndexMatcher = alterIndexPattern.matcher(upScript);

        while (alterIndexMatcher.find()) {
            indexes.add(alterIndexMatcher.group(1));
        }

        return indexes.stream()
                .distinct()
                .toList();
    }

    public List<String> extractPrimaryKeys(String script) {

        List<String> primaryKeys = new ArrayList<>();

        Pattern pattern = Pattern.compile(
                "(\\w+)\\s+[^,\\n]+\\s+PRIMARY\\s+KEY",
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(script);

        while (matcher.find()) {
            primaryKeys.add(matcher.group(1));
        }

        return primaryKeys;
    }

    public void validateSqlSyntax(Connection connection, String upScript)
            throws ValidationException {

        String sql = extractActualSql(upScript);

        if (sql.isBlank()) {
            throw new ValidationException("UP script is empty.");
        }

        boolean originalAutoCommit;

        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }

            // Undo any changes made during validation
            connection.rollback();

            // Restore original state
            connection.setAutoCommit(originalAutoCommit);

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }

            throw new ValidationException(
                    "SQL syntax error: " + e.getMessage()
            );
        }
    }

    private String extractActualSql(String upScript) {

        String[] lines = upScript.split("\\R");
        StringBuilder actualSql = new StringBuilder();

        // Skip template header
        for (int i = 4; i < lines.length; i++) {
            actualSql.append(lines[i]).append("\n");
        }

        return actualSql.toString().trim();
    }

    public void updateChecksum(String version, String upScript) {

    }
}

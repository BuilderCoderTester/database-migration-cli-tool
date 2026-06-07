package com.project.demo.utility;

import com.project.demo.component.*;
import com.project.demo.dto.ConnectionRequest;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.MigrationRepository;
import com.project.demo.service.MigrationService;
import jakarta.transaction.Status;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@AllArgsConstructor
@Component
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
        System.out.println("cominng to the office baby");
        Connection connection = activeConnection(connectionContext.getCurrentDatabase());
        System.out.println("current databse = "+connectionContext.getCurrentDatabase());
        sqlExecutor.executeScript(script.getUpScript(),connection,connectionContext.getCurrentDatabase());
        System.out.println("hehe he ami sei checl je kaj kori");
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

        Connection newConnection = DriverManager.getConnection(
                dbUrl,
                "postgres",
                "sigilotech"
        );
        return newConnection;
    }
    /// history of migration records
    public List<Migration> getMigrationHistory(Long connectionId,String currentDatabase) throws SQLException {
        Connection connection = activeConnection(currentDatabase);
        return repository.findAll(connectionId,connection);
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
}

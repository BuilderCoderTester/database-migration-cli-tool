package com.project.demo.utility;

import com.project.demo.component.MigrationValidator;
import com.project.demo.component.SqlExecutor;
import com.project.demo.component.MigrationLoader;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.MigrationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Component
public class Helper {

    private final SqlExecutor sqlExecutor;
    private final MigrationRepository repository;
    private final MigrationValidator validator;
    private final MigrationLoader migrationLoader;


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
    public void applyVersioned(MigrationScript script, long start, Long connectionId) {
        sqlExecutor.executeScript(script.getUpScript());
        saveMigrationRecord(script, connectionId,System.currentTimeMillis() - start, false);
    }

    /// SAVE THE MIGRATION RECORDS
    public void saveMigrationRecord(MigrationScript script,Long connectionId,
                                    long executionTime,
                                    boolean repeatable) {

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

        repository.save(m,connectionId);
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
    public Optional<String> getCurrentVersion(Long connectionId) {
        return repository.findLastSuccessful(connectionId).map(Migration::getVersion);
    }

    /// history of migration records
    public List<Migration> getMigrationHistory(Long connectionId) {
        return repository.findAll(connectionId);
    }

    ///  checksum validation
    public boolean validateChecksum(String version, String script) {
        Optional<Migration> existing = repository.findByVersion(version);
        if (existing.isEmpty()) return true;

        String currentChecksum = calculateChecksum(script);
        return currentChecksum.equals(existing.get().getChecksum());
    }

    /// database dirty
    public boolean isDatabaseDirty() {
        return validator.validateDirtyDb();
    }

    /// pending migration request
    public int getPendingCount(Long connectionId) {
        try {
            String currentVersion = getCurrentVersion(connectionId).orElse(null);
            return migrationLoader.loadPendingMigrations(currentVersion,connectionId).size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch pending migrations", e);
        }
    }


}

package com.project.demo.modules.migration.repository;

import com.project.demo.component.SqlExecutor;
import com.project.demo.config.MigrationProperties;
import com.project.demo.enumuration.Status;
import com.project.demo.modules.migration.dto.MigrationDetailsDTO;
import com.project.demo.modules.migration.mappingProfile.MigrationMapper;
import com.project.demo.modules.migration.model.ConnectionConfig;
import com.project.demo.modules.migration.model.Migration;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.infrastructure.sqlQueries.MigrationQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
@Slf4j
public class MigrationRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final Pattern VERSIONED_PATTERN =
            Pattern.compile("V(\\d+)__([\\w_]+)\\.sql");
    private MigrationProperties properties;
    private SqlExecutor sqlExecutor;

    public MigrationRepository(DataSource dataSource, MigrationProperties properties, SqlExecutor sqlExecutor) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.properties = properties;
        this.sqlExecutor = sqlExecutor;
    }

    public void createSchemaHistoryTable() {
        String sql = """
                
                    CREATE TABLE IF NOT EXISTS migration (
                     id BIGSERIAL PRIMARY KEY,
                
                     version VARCHAR(50) NOT NULL UNIQUE,
                     description VARCHAR(255),
                
                     script TEXT,
                     checksum VARCHAR(64),
                
                     executed_at TIMESTAMP,
                     execution_time BIGINT,
                
                     success BOOLEAN DEFAULT FALSE,
                
                     error_message TEXT,
                     error_stack_trace TEXT,
                
                     retry_count INT DEFAULT 0,
                
                     dirty BOOLEAN DEFAULT FALSE,
                     repeatable BOOLEAN DEFAULT FALSE,
                
                     name VARCHAR(255),
                     connection_id BIGINT,  -- ✅ correct type
                
                         CONSTRAINT fk_connection
                             FOREIGN KEY (connection_id)
                             REFERENCES connections(connection_id)
                             ON DELETE CASCADE
                 );
                """;
        jdbcTemplate.execute(sql);

        String check = """
                CREATE TABLE IF NOT EXISTS migration_lock (
                    id VARCHAR(255) PRIMARY KEY,
                    locked BOOLEAN DEFAULT FALSE,
                    locked_at TIMESTAMP,
                    locked_by VARCHAR(255)
                    );
                """;
        try {
            jdbcTemplate.execute(check);
            log.info("Migration lock table is ready");
        } catch (Exception e) {
            log.error("Failed to create migration lock table", e);
        }
    }

    @Transactional
    public void save(Migration migration,
                     Long connectionId,
                     Connection connection) throws SQLException {

        try (PreparedStatement debugStmt =
                     connection.prepareStatement("SELECT current_database()");
             ResultSet rs = debugStmt.executeQuery()) {

            if (rs.next()) {
                System.out.println("🔥 Connected DB: " + rs.getString(1));
            }
        }

        try (PreparedStatement stmt =
                     connection.prepareStatement(MigrationQuery.INSERT_INTO_SUB_MIGRATION)) {

            // version
            stmt.setString(1, migration.getVersion());

            // description
            stmt.setString(2, migration.getDescription());

            // script
            stmt.setString(3, migration.getScript());

            // checksum
            stmt.setString(4, migration.getChecksum());

            // created_at
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            // executed_at
            if (migration.getExecutedAt() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(migration.getExecutedAt()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }

            // running_time
            if (migration.getExecutionTime() != null) {
                stmt.setLong(7, migration.getExecutionTime());
            } else {
                stmt.setNull(7, Types.BIGINT);
            }

            // execution_success
            stmt.setBoolean(8, false);

            // status (PostgreSQL enum)
            stmt.setObject(9, migration.getStatus().name(), Types.OTHER);

            // connection_id
            stmt.setLong(10, connectionId);

            System.out.println("========================================");
            System.out.println("Saving Migration");
            System.out.println("Version       : " + migration.getVersion());
            System.out.println("Connection ID : " + connectionId);
            System.out.println("Status        : " + migration.getStatus());
            System.out.println("Success       : " + false);
            System.out.println("========================================");

            int rows = stmt.executeUpdate();

            System.out.println("Rows Affected : " + rows);
            System.out.println("Migration saved successfully.");

        } catch (SQLException e) {

            System.out.println("========================================");
            System.out.println("Failed to save migration");
            System.out.println("Version       : " + migration.getVersion());
            System.out.println("Connection ID : " + connectionId);
            e.printStackTrace();
            System.out.println("========================================");

            throw e;
        }
    }

    public List<Migration> findAll(Long connectionId,
                                   Connection connection, String DatabaseName)
            throws SQLException {

        String sql = """
                SELECT *
                FROM sub_migration
                WHERE connection_id = ?
                """;

        List<Migration> migrations = new ArrayList<>();

        try (PreparedStatement stmt =
                     connection.prepareStatement(sql)) {

            stmt.setLong(1, connectionId);

            log.debug("Loading migrations for connection {}", connectionId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    log.trace("Mapping migration version {}", rs.getString("version"));

                    Migration migration = new Migration();

                    migration.setVersion(rs.getString("version"));

                    migration.setDescription(rs.getString("description"));

                    migration.setScript(rs.getString("script"));

                    migration.setChecksum(rs.getString("checksum"));

                    Timestamp executedAt =
                            rs.getTimestamp("executed_at");

                    if (executedAt != null) {
                        migration.setExecutedAt(executedAt.toLocalDateTime());
                    }

                    migration.setExecutionTime(
                            rs.getLong("execution_time")
                    );

                    migration.setSuccess(
                            rs.getBoolean("success")
                    );

                    ConnectionConfig connObj = new ConnectionConfig();

                    connObj.setConnectionId(
                            rs.getLong("connection_id")
                    );
                    connObj.setName("localhost");
                    connObj.setDatabase(DatabaseName);

                    migration.setConnection(connObj);

                    // ADD TO LIST
                    migrations.add(migration);
                }
            }
        }

        System.out.println(
                "migration count = " + migrations.size()
        );

        return migrations;
    }

    public Optional<Migration> findByVersion(String version, Connection connection) throws SQLException {
        String sql = "SELECT * FROM sub_migration WHERE version = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, version);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Migration migration = new Migration();

                    migration.setVersion(rs.getString("version"));
                    migration.setDescription(rs.getString("description"));
                    migration.setChecksum(rs.getString("checksum"));
                    migration.setSuccess(rs.getBoolean("success"));

                    return Optional.of(migration);
                }
            }
        }

        return Optional.empty();
    }

    //FIND THE LAST SUCCESSFUL MIGRATION FILE OR SCRIPT
    public Optional<Migration> findLastSuccessful(Long connectionId, Connection connection) throws SQLException {

        String sql = """
                SELECT *
                FROM sub_migration
                WHERE success = true
                AND connection_id = ?
                ORDER BY version DESC
                LIMIT 1
                """;

        // Debug current DB
        PreparedStatement debugStmt = connection.prepareStatement("SELECT current_database()");
        ResultSet debugRs = debugStmt.executeQuery();

        if (debugRs.next()) {
            System.out.println("🔥 Connected to: " + debugRs.getString(1));
        }

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, connectionId);

        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            Migration migration = new MigrationRowMapper().mapRow(rs, 1);
            return Optional.of(migration);
        }

        return Optional.empty();
    }

    public MigrationScript findFailedMigrations(String versionId, Long connectionId) {
        return null;
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

    public void clearDirtyFlag(String version) {

        String sql = """
                    UPDATE migrations
                    SET dirty = false
                    WHERE version = ?
                """;

        jdbcTemplate.update(sql, version);
    }

    public void markAsRepaired(String version, MigrationScript script, Connection conn, String databaseName, long connectionId) throws SQLException {

        sqlExecutor.executeScript(script.getUpScript(), conn, databaseName);
        saveMigrationRecord(script, connectionId, System.currentTimeMillis(), false, conn);

    }

    public void saveMigrationRecord(MigrationScript script, Long connectionId,
                                    long executionTime,
                                    boolean repeatable, Connection connection) throws SQLException {

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

        save(m, connectionId, connection);
    }

    public void saveCreatedMigration(MigrationScript script, Long connectionId, Connection connection, StringBuilder content, Status status) throws SQLException {

        Migration migration = new Migration();

        migration.setVersion(script.getVersion());
        migration.setDescription(script.getDescription());
        migration.setName(script.getName());
        migration.setChecksum(calculateChecksum(script.getUpScript()));
        migration.setScript(String.valueOf(content));
        migration.setSuccess(false);
        migration.setStatus(status);
        migration.setDirty(false);
        migration.setRepeatable(false);
        migration.setExecutionTime(System.currentTimeMillis());
        migration.setRunningTime(0L);
        save(migration, connectionId, connection);
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

    @Transactional
    public void deleteByVersion(String version) {
        String sql = "DELETE FROM sub_migration WHERE version = ?";
        jdbcTemplate.update(sql, version);
    }

    @Transactional
    public boolean existsByVersion(String version) {
        String sql = "SELECT COUNT(*) FROM sub_migration WHERE version = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, version);
        return count != null && count > 0;
    }

    @Transactional
    public Optional<Migration> findById(String version) {
        String sql = "SELECT * FROM sub_migration WHERE version = ?";

        try {
            Migration migration = jdbcTemplate.queryForObject(
                    sql,
                    new MigrationRowMapper(),
                    version
            );
            return Optional.ofNullable(migration);

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public boolean existsByDirtyTrue() {
        String sql = "SELECT COUNT(*) FROM sub_migration";
        Integer count = 0;
        try {
            count = jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            System.out.println("exception");
        }
        return count != null && count > 0;

    }

    @Transactional
    public boolean existsById(String version) {
        return existsByVersion(version);
    }

    @Transactional
    public Optional<Migration> findTopByOrderByExecutedAtDesc() {

        String sql = """
                    SELECT * FROM sub_migration
                    ORDER BY executed_at DESC
                    LIMIT 1;
                """;

        try {
            Migration migration = jdbcTemplate.queryForObject(
                    sql,
                    new MigrationRowMapper()
            );
            return Optional.ofNullable(migration);

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void saveSuccess(MigrationScript script, Long connectionId, long l) {
    }

    public void saveFailure(MigrationScript script, Long connectionId, Exception e) {

    }

    public MigrationDetailsDTO loadMigrationScriptDetails(String versionId, Long connectionId, Connection connection) throws SQLException {
        PreparedStatement statement =
                connection.prepareStatement(MigrationQuery.GET_MIGRATION_SCRIPT_DETAILS);

        statement.setLong(1, connectionId);
        statement.setString(2, versionId);

        ResultSet resultSet = statement.executeQuery();

        Migration actualScript = null;

        if (resultSet.next()) {
            actualScript = new Migration();

            actualScript.setVersion(resultSet.getString("version"));
            actualScript.setDescription(resultSet.getString("description"));
            actualScript.setScript(resultSet.getString("script"));
            actualScript.setChecksum(resultSet.getString("checksum"));
            Timestamp executedAt = resultSet.getTimestamp("executed_at");

            actualScript.setExecutedAt(
                    executedAt != null ? executedAt.toLocalDateTime() : null
            );
            actualScript.setExecutionTime(resultSet.getLong("execution_time"));
            actualScript.setSuccess(resultSet.getBoolean("success"));
            actualScript.setErrorMessage(resultSet.getString("error_message"));
            actualScript.setErrorStackTrace(resultSet.getString("error_stack_trace"));
            actualScript.setRetryCount(resultSet.getInt("retry_count"));
            actualScript.setDirty(resultSet.getBoolean("dirty"));
            actualScript.setRepeatable(resultSet.getBoolean("repeatable"));
            actualScript.setName(resultSet.getString("name"));
        }

        return MigrationMapper.toDto(actualScript);

    }

    public List<Migration> loadAllPendingMigrationScript(
            Connection connection,
            Long connectionId) throws SQLException {

        List<Migration> migrations = new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(MigrationQuery.GET_PENDING_MIGRATION_SCRIPT)) {

            statement.setLong(1, connectionId);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    Migration migration = new Migration();

                    migration.setVersion(rs.getString("version"));
                    migration.setDescription(rs.getString("description"));
                    migration.setScript(rs.getString("script"));
                    migration.setChecksum(rs.getString("checksum"));

                    Timestamp executedAt = rs.getTimestamp("executed_at");
                    if (executedAt != null) {
                        migration.setExecutedAt(executedAt.toLocalDateTime());
                    }

                    migration.setExecutionTime(rs.getLong("execution_time"));
                    migration.setSuccess(rs.getBoolean("success"));
                    migration.setErrorMessage(rs.getString("error_message"));
                    migration.setErrorStackTrace(rs.getString("error_stack_trace"));
                    migration.setRetryCount(rs.getInt("retry_count"));
                    migration.setDirty(rs.getBoolean("dirty"));
                    migration.setRepeatable(rs.getBoolean("repeatable"));
                    migration.setName(rs.getString("name"));
                    ConnectionConfig connectionConfig = new ConnectionConfig();
                    connectionConfig.setConnectionId(rs.getLong("connection_id"));
                    migration.setConnection(connectionConfig);

                    migrations.add(migration);
                }
            }
        }

        return migrations;
    }

    public List<MigrationScript> getFailedMigrations(Connection connection) {
        return  null;
    }

    private static class MigrationRowMapper implements RowMapper<Migration> {
        @Override
        public Migration mapRow(ResultSet rs, int rowNum) throws SQLException {
            Migration m = new Migration();
            m.setVersion(rs.getString("version"));
            System.out.println(m.getVersion());
            m.setDescription(rs.getString("description"));
            System.out.println(m.getDescription());
            m.setScript(rs.getString("script"));
            System.out.println(m.getScript());
            m.setChecksum(rs.getString("checksum"));
            System.out.println(m.getChecksum());
            m.setExecutedAt(rs.getTimestamp("executed_at") != null ?
                    rs.getTimestamp("executed_at").toLocalDateTime() : null);
            System.out.println(m.getExecutedAt());

            m.setExecutionTime(rs.getLong("execution_time"));
            System.out.println(m.getExecutionTime());

            m.setSuccess(rs.getBoolean("success"));
            System.out.println(m.getSuccess());
            m.setErrorMessage(rs.getString("error_message"));
            System.out.println(m.getErrorMessage());
            m.setErrorStackTrace(rs.getString("error_stack_trace"));
            System.out.println(m.getErrorStackTrace());
            m.setRetryCount(rs.getInt("retry_count"));
            System.out.println(m.getRetryCount());
            m.setDirty(rs.getBoolean("dirty"));
            m.setRepeatable(rs.getBoolean("repeatable"));
            m.setName(rs.getString("name"));

            return m;
        }
    }


}

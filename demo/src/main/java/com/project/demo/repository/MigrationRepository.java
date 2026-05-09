// MigrationRepository.java
package com.project.demo.repository;

import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.utility.ConnectionHolder;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MigrationRepository {

    private final JdbcTemplate jdbcTemplate;

    public MigrationRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
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
            System.out.println("MIGRATION LOCK TABLE IS CREATED! ");
        } catch (Exception e) {
            System.out.println("Creation Querry is not working.");
        }
    }

    @Transactional
    public void save(Migration migration, Long connectionId) {
        System.out.println("come-2");
        String sql = """
                INSERT INTO sub_migration (
                    version,
                    description,
                    script,
                    checksum,
                    executed_at,
                    execution_time,
                    success,
                    connection_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (version)
                DO UPDATE SET
                    success = EXCLUDED.success,
                    execution_time = EXCLUDED.execution_time
                """;

        Connection conn = ConnectionHolder.get();

        System.out.println("Using connection: " + conn);

        try (PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, migration.getVersion());
            pst.setString(2, migration.getDescription());
            pst.setString(3, migration.getScript());
            pst.setString(4, migration.getChecksum());

            if (migration.getExecutedAt() != null) {
                pst.setTimestamp(5, Timestamp.valueOf(migration.getExecutedAt()));
            } else {
                pst.setTimestamp(5, null);
            }

            pst.setLong(6, migration.getExecutionTime());
            pst.setBoolean(7, migration.isSuccess());
            pst.setLong(8, connectionId);

            int rows = pst.executeUpdate();

            System.out.println("Rows affected: " + rows);
        } catch (SQLException e) {
            System.out.println("not runnig");
            throw new RuntimeException(e);
        }
    }

    public List<Migration> findAll(Long connectionId) {
        String sql = "SELECT * FROM migration WHERE connection_id = ? ORDER BY version";
        return jdbcTemplate.query(sql, new MigrationRowMapper(), connectionId);
    }

    public Optional<Migration> findByVersion(String version) {
        String sql = "SELECT * FROM migration WHERE version = ?";
        List<Migration> results = jdbcTemplate.query(sql, new MigrationRowMapper(), version);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    //FIND THE LAST SUCCESSFUL MIGRATION FILE OR SCRIPT
    public Optional<Migration> findLastSuccessful(Long connectionId) throws SQLException {
        String sql = """
                SELECT * 
                FROM sub_migration
                WHERE success = true
                  AND connection_id = ?
                ORDER BY version DESC
                LIMIT 1
                """;

        Connection conn = ConnectionHolder.get();

        System.out.println("The real connection: " + conn);

        try (
                PreparedStatement dbPst = conn.prepareStatement("SELECT current_database()");
                ResultSet dbRs = dbPst.executeQuery()
        ) {

            if (dbRs.next()) {
                System.out.println("🔥 Connected to: " + dbRs.getString(1));
            }
        }

        try (
                PreparedStatement pst = conn.prepareStatement(sql)
        ) {

            pst.setLong(1, connectionId);

            try (ResultSet rs = pst.executeQuery()) {

                if (rs.next()) {

                    Migration migration = new Migration();

                    migration.setVersion(rs.getString("version"));
                    migration.setDescription(rs.getString("description"));
                    migration.setScript(rs.getString("script"));
                    migration.setChecksum(rs.getString("checksum"));
                    migration.setExecutedAt(rs.getTimestamp("executed_at").toLocalDateTime());
                    migration.setExecutionTime(rs.getLong("execution_time"));
                    migration.setSuccess(rs.getBoolean("success"));
                    migration.setConnectionId(rs.getLong("connection_id"));

                    return Optional.of(migration);
                }
            }
        }

        return Optional.empty();
    }

    public List<Migration> findFailedMigrations() {

        String sql = "SELECT * FROM migrations WHERE success = false";

        return jdbcTemplate.query(sql, new MigrationRowMapper());
    }

    public void clearDirtyFlag(String version) {

        String sql = """
                    UPDATE migrations
                    SET dirty = false
                    WHERE version = ?
                """;

        jdbcTemplate.update(sql, version);
    }

    public void markAsRepaired(String version) {

        String sql = """
                    UPDATE migrations
                    SET success = false,
                        dirty = false
                    WHERE version = ?
                """;

        jdbcTemplate.update(sql, version);
    }

    @Transactional
    public void deleteByVersion(String version) {
        String sql = "DELETE FROM migration WHERE version = ?";
        jdbcTemplate.update(sql, version);
    }

    @Transactional
    public boolean existsByVersion(String version) {
        String sql = "SELECT COUNT(*) FROM migration WHERE version = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, version);
        return count != null && count > 0;
    }

    @Transactional
    public Optional<Migration> findById(String version) {
        String sql = "SELECT * FROM migration WHERE version = ?";

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
        String sql = "SELECT COUNT(*) FROM migration ;";
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
                    SELECT * FROM migration
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
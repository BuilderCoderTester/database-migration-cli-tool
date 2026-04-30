// MigrationRepository.java
package com.project.demo.repository;

import com.project.demo.model.Migration;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                
                     name VARCHAR(255)
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
        try{
            jdbcTemplate.execute(check);
            System.out.println("MIGRATION LOCK TABLE IS CREATED! ");
        }catch (Exception e ){
            System.out.println("Creation Querry is not working.");
        }
    }

    @Transactional
    public void save(Migration migration) {

        String sql = """
            INSERT INTO migration ( version, description, script, checksum, 
                                       executed_at, execution_time, success)
            VALUES ( ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (version) DO UPDATE SET
                success = EXCLUDED.success,
                execution_time = EXCLUDED.execution_time
            """;

        jdbcTemplate.update(sql,
                migration.getVersion(),
                migration.getDescription(),
                migration.getScript(),
                migration.getChecksum(),
                migration.getExecutedAt(),
                migration.getExecutionTime(),
                migration.isSuccess()
        );
    }

    public List<Migration> findAll() {
        String sql = "SELECT * FROM migration ORDER BY version";
        return jdbcTemplate.query(sql, new MigrationRowMapper());
    }

    public Optional<Migration> findByVersion(String version) {
        String sql = "SELECT * FROM migration WHERE version = ?";
        List<Migration> results = jdbcTemplate.query(sql, new MigrationRowMapper(), version);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Migration> findLastSuccessful() {
        String sql = "SELECT * FROM migration WHERE success = true ORDER BY version DESC LIMIT 1";
        List<Migration> results = jdbcTemplate.query(sql, new MigrationRowMapper());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
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
        Integer count= 0;
        try {
             count = jdbcTemplate.queryForObject(sql, Integer.class);
        }catch (Exception e){
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
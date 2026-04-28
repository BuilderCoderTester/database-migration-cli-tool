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
                id BIGSERIAL PRIMARY KEY ,
                version VARCHAR(50) NOT NULL,
                description VARCHAR(255),
                script TEXT,
                checksum VARCHAR(64),
                executed_at TIMESTAMP,
                execution_time BIGINT,
                success BOOLEAN DEFAULT FALSE
            )
            """;
        jdbcTemplate.execute(sql);
    }

    @Transactional
    public void save(Migration migration) {
        String sql = """
            INSERT INTO migration ( version, description, script, checksum, 
                                       executed_at, execution_time, success)
            VALUES ( ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
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
        String sql = "SELECT * FROM schema_history ORDER BY version";
        return jdbcTemplate.query(sql, new MigrationRowMapper());
    }

    public Optional<Migration> findByVersion(String version) {
        String sql = "SELECT * FROM schema_history WHERE version = ?";
        List<Migration> results = jdbcTemplate.query(sql, new MigrationRowMapper(), version);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Migration> findLastSuccessful() {
        String sql = "SELECT * FROM schema_history WHERE success = true ORDER BY version DESC LIMIT 1";
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
        String sql = "DELETE FROM schema_history WHERE version = ?";
        jdbcTemplate.update(sql, version);
    }

    @Transactional
    public boolean existsByVersion(String version) {
        String sql = "SELECT COUNT(*) FROM migrations WHERE version = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, version);
        return count != null && count > 0;
    }

    @Transactional
    public Optional<Migration> findById(String version) {
        System.out.println("reach point -2 " + version );
        String sql = "SELECT * FROM migration WHERE version = ?";

        try {
            System.out.println("eseche");
            Migration migration = jdbcTemplate.queryForObject(
                    sql,
                    new MigrationRowMapper(),
                    version
            );
            System.out.println("the migrateoin after = " + migration);
            return Optional.ofNullable(migration);

        } catch (EmptyResultDataAccessException e) {
            System.out.println("at the cache system .");
            return Optional.empty();
        }
    }

    @Transactional
    public boolean existsByDirtyTrue() {
        System.out.println("come");
        String sql = "SELECT COUNT(*) FROM migration ;";
        Integer count= 0;
        try {
             count = jdbcTemplate.queryForObject(sql, Integer.class);
            System.out.println("come -2");
        }catch (Exception e){
            System.out.println("exception");
        }
        System.out.println("come-3" + count);
        return count != null && count > 0;

    }

    @Transactional
    public boolean existsById(String version) {
        return existsByVersion(version);
    }

    @Transactional
    public Optional<Migration> findTopByOrderByExecutedAtDesc() {

        String sql = """
            SELECT * FROM migrations
            ORDER BY executed_at DESC
            LIMIT 1
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
            m.setDescription(rs.getString("description"));
            m.setScript(rs.getString("script"));
            m.setChecksum(rs.getString("checksum"));
            m.setExecutedAt(rs.getTimestamp("executed_at") != null ?
                    rs.getTimestamp("executed_at").toLocalDateTime() : null);
            m.setExecutionTime(rs.getLong("execution_time"));
            m.setSuccess(rs.getBoolean("success"));
            return m;
        }
    }
}
package com.project.demo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Repository
public class MigrationLockRepository {

    private final JdbcTemplate jdbcTemplate;

    public MigrationLockRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public int acquireLock(Long connectionId, String lockedBy, Timestamp timeout) {

        String sql = """
        UPDATE migration_lock
        SET locked = true,
            locked_at = CURRENT_TIMESTAMP,
            locked_by = ?
        WHERE connection_id = ?
          AND (locked = false OR locked_at < ?)
    """;

        return jdbcTemplate.update(sql, lockedBy, connectionId, timeout);
    }

    @Transactional
    public int releaseLock(Long connectionId, String lockedBy) {

        String sql = """
        UPDATE migration_lock
        SET locked = false,
            locked_at = NULL,
            locked_by = NULL
        WHERE connection_id = ?
          AND locked_by = ?
    """;

        return jdbcTemplate.update(sql, connectionId, lockedBy);
    }
}

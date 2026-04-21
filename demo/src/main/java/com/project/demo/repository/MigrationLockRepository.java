package com.project.demo.repository;

import com.project.demo.model.Migration;
import com.project.demo.model.MigrationLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class MigrationLockRepository {

    private final JdbcTemplate jdbcTemplate;

    public MigrationLockRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Boolean holdLock(String lockedBy) {

//        check whether the tables exist or not
        String check = """
                CREATE TABLE IF NOT EXISTS migration_lock (
                    id VARCHAR(255) PRIMARY KEY,
                    locked BOOLEAN DEFAULT FALSE,
                    locked_at TIMESTAMP,
                    locked_by VARCHAR(255)
                    )
                """;
        jdbcTemplate.execute(check);

        String sql = """
                    UPDATE migration_lock
                    SET locked = true,
                        locked_at = CURRENT_TIMESTAMP,
                        locked_by = ?
                    WHERE id = 1
                      AND (
                          locked = false 
                          OR locked_at < ?
                      )
                """;


        Timestamp timeout = Timestamp.valueOf(
                LocalDateTime.now().minusMinutes(10)
        );

        int updated = jdbcTemplate.update(sql, lockedBy, timeout);
        if (updated == 0) {
            throw new RuntimeException(
                    "Migration lock not acquired. Another process is running."
            );
        }
        return true;
    }

    @Transactional
    public void releaseLock() {

        String sql = """
                    UPDATE migration_lock
                    SET locked = false,
                        locked_at = NULL,
                        locked_by = NULL
                    WHERE id = 1
                """;

        jdbcTemplate.update(sql);
    }
}

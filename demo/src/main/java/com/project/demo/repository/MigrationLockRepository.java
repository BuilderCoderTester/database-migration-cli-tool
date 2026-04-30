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
    public void holdLock(String lockedBy) {
        System.out.println("THE LOCKED IS HOLD BY : "+lockedBy);
        String sql = """
                    UPDATE migration_lock
                    SET locked = true,
                        locked_at = CURRENT_TIMESTAMP,
                        locked_by = ?
                    ;
               """;


        Timestamp timeout = Timestamp.valueOf(
                LocalDateTime.now().minusMinutes(10)
        );

        int updated = jdbcTemplate.update(sql, lockedBy);
        // * this is a bug , not yet solved.
        if (updated != 0) {
            throw new RuntimeException(
                    "Migration lock not acquired. Another process is running."
            );
        }
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

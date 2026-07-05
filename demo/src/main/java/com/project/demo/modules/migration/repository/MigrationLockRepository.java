package com.project.demo.modules.migration.repository;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class MigrationLockRepository {

    /**
     * REAL DATABASE LOCK
     * <p>
     * This method blocks other transactions
     * trying to acquire the same lock row.
     *
     * @return
     */
    public boolean acquireLock(Connection connection,
                               Long connectionId) throws SQLException {

        String sql = """
                SELECT connection_id
                FROM migration_lock
                WHERE connection_id = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, connectionId);

            statement.executeQuery();
        }
        return false;
    }

    /**
     * OPTIONAL METADATA UPDATE
     */
    public void markLocked(Connection connection,
                           Long connectionId,
                           String lockedBy) throws SQLException {

        String sql = """
                UPDATE migration_lock
                SET locked = true,
                    locked_at = CURRENT_TIMESTAMP,
                    locked_by = ?,
                    heartbeat_at = CURRENT_TIMESTAMP
                WHERE connection_id = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, lockedBy);
            statement.setLong(2, connectionId);

            statement.executeUpdate();
        }
    }

    /**
     * OPTIONAL RELEASE METADATA
     *
     * @return
     */
    public boolean releaseLock(Connection connection,
                               Long connectionId) throws SQLException {

        String sql = """
                UPDATE migration_lock
                SET locked = false,
                    locked_at = NULL,
                    locked_by = NULL,
                    heartbeat_at = NULL,
                    last_completed_time = CURRENT_TIMESTAMP
                WHERE connection_id = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, connectionId);

            statement.executeUpdate();
        }
        return false;
    }
}
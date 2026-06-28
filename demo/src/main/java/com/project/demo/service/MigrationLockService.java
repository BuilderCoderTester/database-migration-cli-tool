package com.project.demo.service;

import com.project.demo.repository.MigrationLockRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class MigrationLockService {

    private final MigrationLockRepository lockRepository;

    // CONSTRUCTOR
    public MigrationLockService( MigrationLockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    // LOCK HOLDING OPERATION
    @Transactional
    public String acquireLock(Connection connection, Long connectionId) throws SQLException {
        log.debug("Attempting to acquire migration lock for connection {}", connectionId);
        String lockedBy = getHostName() + "-" + UUID.randomUUID();

        Timestamp timeout = Timestamp.valueOf(
                LocalDateTime.now().minusMinutes(10)
        );

        boolean updated = lockRepository.acquireLock(connection,connectionId);
        lockRepository.markLocked(connection,connectionId,lockedBy);
        setLockTimeout(connection);
        if (updated) {
            throw new RuntimeException(
                    "Another migration is already running on this database (connectionId="
                            + connectionId + ")"
            );
        }

        log.info("Migration lock acquired for connection {} by {}", connectionId, lockedBy);

        return lockedBy;
    }
    public void setLockTimeout(Connection connection)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "SET lock_timeout = '30s'"
                     )) {

            statement.execute();
        }
    }
    // LOCK RELEASE OPERATION
    public void releaseLock(Connection connection, Long connectionId, String lockedBy) throws Exception {

        boolean updated = lockRepository.releaseLock(connection,connectionId);

        if (updated) {
            throw new RuntimeException(
                    "Failed to release lock. It may be owned by another process."
            );
        }

        log.info("Migration lock released for connection {} by {}", connectionId, lockedBy);
    }
    public boolean isLockStale(Connection connection)
            throws SQLException {
        log.debug("Checking migration lock heartbeat");
        String sql = """
            SELECT heartbeat_at
            FROM migration_lock
            WHERE connection_id = 1
            """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {

                Timestamp heartbeat =
                        rs.getTimestamp("heartbeat_at");

                if (heartbeat == null) {
                    return false;
                }

                long diff =
                        System.currentTimeMillis()
                                - heartbeat.getTime();

                // stale after 60 seconds
                return diff > 60000;
            }
        }

        return false;
    }
    public void clearStaleLock(Connection connection)
            throws SQLException {

        String sql = """
            UPDATE migration_lock
            SET locked = false,
                locked_by = NULL,
                locked_at = NULL,
                heartbeat_at = NULL
            WHERE id = 1
            """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.executeUpdate();
        }
    }
    public void updateHeartbeat(Connection connection)
            throws SQLException {

        String sql = """
            UPDATE migration_lock
            SET heartbeat_at = CURRENT_TIMESTAMP
            WHERE connection_id = 1
            """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.executeUpdate();
        }
    }
    // GET CURRENT HOST
    public String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

}

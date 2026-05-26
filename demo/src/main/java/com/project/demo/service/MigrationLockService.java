package com.project.demo.service;

import com.project.demo.repository.MigrationLockRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MigrationLockService {

    private final MigrationLockRepository lockRepository;

    // CONSTRUCTOR
    public MigrationLockService(
            MigrationLockRepository lockRepository
    ) {
        this.lockRepository = lockRepository;
    }

    // =====================================================
    // ACQUIRE LOCK
    // =====================================================

    @Transactional
    public String acquireLock(
            Connection connection,
            Long connectionId
    ) throws SQLException {

        String lockedBy =
                getHostName() + "-"
                        + UUID.randomUUID();

        boolean alreadyLocked =
                lockRepository.acquireLock(
                        connection,
                        connectionId
                );

        if (alreadyLocked) {

            if (isLockStale(
                    connection,
                    connectionId
            )) {

                clearStaleLock(
                        connection,
                        connectionId
                );

            } else {

                throw new RuntimeException(
                        "Another migration is already running on this database (connectionId="
                                + connectionId + ")"
                );
            }
        }

        lockRepository.markLocked(
                connection,
                connectionId,
                lockedBy
        );

        setLockTimeout(connection);

        System.out.println(
                "LOCK ACQUIRED | DB="
                        + connectionId
                        + " | by="
                        + lockedBy
        );

        return lockedBy;
    }

    // =====================================================
    // SET LOCK TIMEOUT
    // =====================================================

    public void setLockTimeout(
            Connection connection
    ) throws SQLException {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                "SET lock_timeout = '30s'"
                        )
        ) {

            statement.execute();
        }
    }

    // =====================================================
    // RELEASE LOCK
    // =====================================================

    public void releaseLock(
            Connection connection,
            Long connectionId,
            String lockedBy
    ) throws Exception {

        lockRepository.releaseLock(
                connection,
                connectionId
        );

        System.out.println(
                "LOCK RELEASED | DB="
                        + connectionId
                        + " | by="
                        + lockedBy
        );
    }

    // =====================================================
    // CHECK STALE LOCK
    // =====================================================

    public boolean isLockStale(
            Connection connection,
            Long connectionId
    ) throws SQLException {

        String sql = """
            SELECT heartbeat_at
            FROM migration_lock
            WHERE connection_id = ?
            """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, connectionId);

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                if (rs.next()) {

                    Timestamp heartbeat =
                            rs.getTimestamp(
                                    "heartbeat_at"
                            );

                    if (heartbeat == null) {
                        return false;
                    }

                    long diff =
                            System.currentTimeMillis()
                                    - heartbeat.getTime();

                    // stale after 60 sec
                    return diff > 60000;
                }
            }
        }

        return false;
    }

    // =====================================================
    // CLEAR STALE LOCK
    // =====================================================

    public void clearStaleLock(
            Connection connection,
            Long connectionId
    ) throws SQLException {

        String sql = """
            UPDATE migration_lock
            SET locked = false,
                locked_by = NULL,
                locked_at = NULL,
                heartbeat_at = NULL
            WHERE connection_id = ?
            """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, connectionId);

            statement.executeUpdate();
        }
    }

    // =====================================================
    // UPDATE HEARTBEAT
    // =====================================================

    public void updateHeartbeat(
            Connection connection,
            Long connectionId
    ) throws SQLException {

        String sql = """
            UPDATE migration_lock
            SET heartbeat_at = CURRENT_TIMESTAMP
            WHERE connection_id = ?
            """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, connectionId);

            statement.executeUpdate();
        }
    }

    // =====================================================
    // HOST NAME
    // =====================================================

    public String getHostName() {

        try {

            return java.net.InetAddress
                    .getLocalHost()
                    .getHostName();

        } catch (Exception e) {

            return "unknown";
        }
    }
    
}
package com.project.demo.service;

import com.project.demo.repository.MigrationLockRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MigrationLockService {

    private final MigrationLockRepository lockRepository;

    // CONSTRUCTOR
    public MigrationLockService( MigrationLockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    // LOCK HOLDING OPERATION
    public String acquireLock(Long connectionId, String targetVersion) {

        String lockedBy = getHostName() + "-" + UUID.randomUUID();

        Timestamp timeout = Timestamp.valueOf(
                LocalDateTime.now().minusMinutes(10)
        );

        int updated = lockRepository.acquireLock(connectionId, lockedBy, timeout);

        if (updated == 0) {
            throw new RuntimeException(
                    "Another migration is already running on this database (connectionId="
                            + connectionId + ")"
            );
        }

        System.out.println(
                "LOCK ACQUIRED | DB=" + connectionId +
                        " | version=" + targetVersion +
                        " | by=" + lockedBy
        );

        return lockedBy;
    }

    // LOCK RELEASE OPERATION
    public void releaseLock(Long connectionId, StringBuilder lockedBy) {

        int updated = lockRepository.releaseLock(connectionId, String.valueOf(lockedBy));

        if (updated == 0) {
            throw new RuntimeException(
                    "Failed to release lock. It may be owned by another process."
            );
        }

        System.out.println(
                "LOCK RELEASED | DB=" + connectionId + " | by=" + lockedBy
        );
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

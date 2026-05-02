package com.project.demo.service;

import com.project.demo.repository.MigrationLockRepository;
import org.springframework.stereotype.Service;

@Service
public class MigrationLockService {

    private final MigrationLockRepository repository;

    public MigrationLockService(MigrationLockRepository repository) {
        this.repository = repository;
    }

    public void acquireLock(Long connectionId) {
        repository.holdLock(getHostName());
    }

    public void releaseLock(Long connectionId) {
        repository.releaseLock();
    }

    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}

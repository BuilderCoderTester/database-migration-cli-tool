package com.project.demo.service;

import com.project.demo.model.LogLevel;
import com.project.demo.model.MigrationLogs;
import com.project.demo.repository.MigrationLogRepo;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogService {
    private final MigrationLogRepo migrationLogRepo;

    public LogService(MigrationLogRepo migrationLogRepo) {
        this.migrationLogRepo = migrationLogRepo;
    }

    public void log(String message, LogLevel level) {
        MigrationLogs log = new MigrationLogs();
        log.setMessage(message);
        log.setLevel(level);
        migrationLogRepo.save(log);
    }

    public List<MigrationLogs> getAllActivities() {
        return migrationLogRepo.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }
}

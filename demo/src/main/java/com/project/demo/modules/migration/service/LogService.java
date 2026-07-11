package com.project.demo.modules.migration.service;

import com.project.demo.modules.migration.dto.logs.response.MigrationLogsResponseDto;
import com.project.demo.modules.migration.mappingProfile.LogMapper;
import com.project.demo.enumuration.LogLevel;
import com.project.demo.modules.migration.model.MigrationLogs;
import com.project.demo.modules.migration.repository.MigrationLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogService {
    @Autowired
    private  MigrationLogRepo migrationLogRepo;

    public void log(String message, LogLevel level) {
        MigrationLogs log = new MigrationLogs();
        log.setMessage(message);
        log.setLevel(level);
        migrationLogRepo.save(log);
    }

    public List<MigrationLogsResponseDto> getAllActivities() {
        return LogMapper.logToResponse(
                migrationLogRepo.findAll(
                        Sort.by(Sort.Direction.DESC, "timestamp")
                )
        );
    }

}

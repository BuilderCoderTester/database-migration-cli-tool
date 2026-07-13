package com.project.demo.api.web.Controller;
import com.project.demo.modules.migration.dto.logs.response.MigrationLogsResponseDto;
import com.project.demo.modules.migration.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class LogController {
    @Autowired
    private  LogService logService;

    @GetMapping("/logs")
    public List<MigrationLogsResponseDto> getAllActivities() {
        return logService.getAllActivities();
    }

}

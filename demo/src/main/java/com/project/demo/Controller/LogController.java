package com.project.demo.Controller;
import com.project.demo.model.MigrationLogs;
import com.project.demo.service.LogService;
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
    public List<MigrationLogs> getAllActivities() {
        return logService.getAllActivities();
    }
}

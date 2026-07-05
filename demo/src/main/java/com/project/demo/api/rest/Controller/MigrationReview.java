package com.project.demo.api.rest.Controller;

import com.project.demo.modules.migration.service.MigrationAiService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migration")
@AllArgsConstructor
@CrossOrigin("*")
public class MigrationReview {

    @Autowired
    private MigrationAiService migrationAiService;
    @PostMapping("/review")
    public com.project.demo.modules.migration.dto.MigrationReview review(String sql){
        return migrationAiService.review(sql);
    }
}

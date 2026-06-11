package com.project.demo.Controller;

import com.project.demo.service.MigrationAiService;
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
    public com.project.demo.dto.MigrationReview review(String sql){
        return migrationAiService.review(sql);
    }
}

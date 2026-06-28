package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.ResponseBody;

@Builder
@ResponseBody
@Getter
@Setter
@AllArgsConstructor
public class MigrationDetailsDTO {
    private String version;

    private String description;

    private String type;

    private String status;

    private boolean success;

    private String author;

    private String database;

    private String connectionName;

    private String executedAt;

    private long executionTime;

    private String checksum;

    private String fileName;

    private String filePath;

    private String script;

    private String rollbackScript;

    private boolean rollbackAvailable;

    private ValidationDTO validation;
}

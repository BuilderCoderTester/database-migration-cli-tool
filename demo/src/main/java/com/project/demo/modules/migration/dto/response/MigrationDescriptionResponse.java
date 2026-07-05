package com.project.demo.modules.migration.dto.response;

import lombok.*;

import java.sql.Time;
import java.time.LocalDateTime;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MigrationDescriptionResponse {
    private String name;
    private String description;
    private String version;
    private String script;
    private LocalDateTime executedAt;
    private boolean success;
    private long executionTime;
}

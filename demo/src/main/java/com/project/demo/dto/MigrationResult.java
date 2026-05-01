package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MigrationResult {
    private String message;
    private int successCount;
    private int failedCount;
}

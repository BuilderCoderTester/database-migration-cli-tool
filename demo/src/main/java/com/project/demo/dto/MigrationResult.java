package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
@Getter
@Setter
public class MigrationResult {
    private String message;
    private int successCount;
    private int failedCount;
}

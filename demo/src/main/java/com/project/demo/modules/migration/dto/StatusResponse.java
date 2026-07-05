package com.project.demo.modules.migration.dto;

import com.project.demo.modules.migration.model.MigrationScript;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusResponse {
    private String currentVersion;
    private int pendingCount;
    private List<MigrationScript> pending;
}

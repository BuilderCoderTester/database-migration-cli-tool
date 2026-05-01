package com.project.demo.dto;

import com.project.demo.model.MigrationScript;
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

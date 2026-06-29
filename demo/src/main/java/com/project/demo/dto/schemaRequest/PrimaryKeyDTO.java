package com.project.demo.dto.schemaRequest;

import lombok.Data;

import java.util.List;

@Data
public class PrimaryKeyDTO {
    // Optional: Constraint name
    private String constraintName;

    // Primary key columns (supports composite keys)
    private List<String> columnNames;
}

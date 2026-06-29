package com.project.demo.dto;

import lombok.Data;

@Data
public class RollbackRequest {
    private Long connectionId;
    private String version;
    private String rollbackType;
}

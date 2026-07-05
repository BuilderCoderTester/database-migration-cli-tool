package com.project.demo.modules.migration.dto.connection.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConnectionResponseDto {
    private boolean success;
    private String message;
    private Long connectionId;
}

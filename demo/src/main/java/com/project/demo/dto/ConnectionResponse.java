package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConnectionResponse {
    private boolean success;
    private String message;
    private Long connectionId;
}

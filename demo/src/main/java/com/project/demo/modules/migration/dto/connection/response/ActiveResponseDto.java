package com.project.demo.modules.migration.dto.connection.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActiveResponseDto {
    private boolean success;
    private String message;


    public ActiveResponseDto(boolean success, String connectionSuccessful, Long connectionId) {
    }
}

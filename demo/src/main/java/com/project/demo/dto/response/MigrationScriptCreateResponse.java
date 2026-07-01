package com.project.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationScriptCreateResponse {

    private boolean success;

    private String message;

    private String reason;

    private String version;

    private String fileName;

    public static MigrationScriptCreateResponse success(
            String message,
            String version,
            String fileName) {

        return MigrationScriptCreateResponse.builder()
                .success(true)
                .message(message)
                .version(version)
                .fileName(fileName)
                .build();
    }

    public static MigrationScriptCreateResponse failure(
            String message,
            String reason) {

        return MigrationScriptCreateResponse.builder()
                .success(false)
                .message(message)
                .reason(reason)
                .build();
    }
}
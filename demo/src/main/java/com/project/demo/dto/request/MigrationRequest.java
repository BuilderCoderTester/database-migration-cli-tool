package com.project.demo.dto.request;

import com.project.demo.enumuration.DatabaseOperation;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MigrationRequest {

    private String targetVersion; // optional
    @NotNull
    private Long connectionId;
    @NotNull
    private DatabaseOperation operation;

    public MigrationRequest(Object o, Object o1) {
    }
}

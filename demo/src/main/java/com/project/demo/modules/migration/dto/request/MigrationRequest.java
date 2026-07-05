package com.project.demo.modules.migration.dto.request;

import com.project.demo.enumuration.DatabaseOperation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

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

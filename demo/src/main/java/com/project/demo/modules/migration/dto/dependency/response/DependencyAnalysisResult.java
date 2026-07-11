package com.project.demo.modules.migration.dto.dependency.response;

import com.project.demo.enumuration.AlterOperation;
import com.project.demo.enumuration.DatabaseOperation;
import com.project.demo.modules.migration.model.Dependency;
import lombok.Data;

import java.util.List;

@Data
public class DependencyAnalysisResult {
    private DatabaseOperation databaseOperation;
    private List<Dependency> dependencies;
    private AlterOperation alterOperation;
    public DependencyAnalysisResult(DatabaseOperation operation,
                                    AlterOperation alterOperation, List<Dependency> dependencies) {
        this.databaseOperation = operation;
        this.dependencies = dependencies;
        this.alterOperation = alterOperation;
    }

}

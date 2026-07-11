package com.project.demo.component;

import com.project.demo.modules.migration.dto.MigrationScriptStatus;
import com.project.demo.modules.migration.dto.dependency.response.DependencyAnalysisResult;
import com.project.demo.modules.migration.model.Dependency;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.modules.migration.parser.ASTDependencyExtractor;
import com.project.demo.modules.migration.validator.DependencyValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;

@Component
public class DependencyEngine {
    @Autowired
    private ASTDependencyExtractor extractor;
    @Autowired
    private DependencyValidator validator;

    public MigrationScriptStatus validate(MigrationScript script, Connection conn) throws Exception {
        DependencyAnalysisResult deps = extractor.extract(script.getUpScript());
        System.out.println("Dependency : " + deps);

        return validator.validate(deps, conn);
    }
}

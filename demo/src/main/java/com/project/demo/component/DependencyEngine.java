package com.project.demo.component;

import com.project.demo.dto.MigrationScriptStatus;
import com.project.demo.model.Dependency;
import com.project.demo.model.MigrationScript;
import com.project.demo.parser.ASTDependencyExtractor;
import com.project.demo.validator.DependencyValidator;
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
        List<Dependency> deps = extractor.extract(script.getUpScript());
        System.out.println("Dependency : " + deps);

        return validator.validate(deps, conn);
    }
}

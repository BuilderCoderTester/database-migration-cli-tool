package com.project.demo.service;

import com.project.demo.component.MigrationLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class MigrationScriptService {

    @Autowired
    private MigrationLoader loader;

    public String create(
            @Option(required = true, description = "Migration version") String version,
            @Option(required = true, description = "Description") String description,
            @Option(defaultValue = "", description = "Up SQL") String up,
            @Option(defaultValue = "", description = "Down SQL") String down
    ) {
        try {
            loader.createMigrationFile(version, description, up, down);
            return String.format("✓ Created migration V%s__%s.sql", version, description.replace(" ", "_"));
        } catch (IOException e) {
            return "Failed to create migration: " + e.getMessage();
        }
    }

}

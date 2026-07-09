package com.project.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private static final String CONFIG_DIR =
            System.getenv("ProgramData")
                    + File.separator
                    + "MigrationTool";

    private static final String CONFIG_FILE =
            CONFIG_DIR + File.separator + "config.json";

    private final ObjectMapper mapper = new ObjectMapper();

    public boolean configExists() {

        File file = new File(CONFIG_FILE);

        System.out.println("Config Path : " + file.getAbsolutePath());

        return file.exists();
    }

    public DatabaseConfig load() throws IOException {
        return mapper.readValue(new File(CONFIG_FILE), DatabaseConfig.class);
    }

    public void save(DatabaseConfig config) throws IOException {

        File dir = new File(CONFIG_DIR);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(CONFIG_FILE), config);
    }
}
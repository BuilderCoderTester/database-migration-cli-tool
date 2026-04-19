package com.project.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

    private String path = "./migrations";
    private String table = "schema_history";
    private boolean validateOnMigrate = true;
    private boolean cleanOnValidationError = false;

    // Getters and setters
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getTable() { return table; }
    public void setTable(String table) { this.table = table; }

    public boolean isValidateOnMigrate() { return validateOnMigrate; }
    public void setValidateOnMigrate(boolean validateOnMigrate) { this.validateOnMigrate = validateOnMigrate; }

    public boolean isCleanOnValidationError() { return cleanOnValidationError; }
    public void setCleanOnValidationError(boolean cleanOnValidationError) { this.cleanOnValidationError = cleanOnValidationError; }
}
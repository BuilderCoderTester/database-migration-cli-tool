// MigrationScript.java
package com.project.demo.model;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MigrationScript {
    private String version;
    private String description;
    private String upScript;
    private String downScript;
    private String fileName;
    private boolean repeatable;
    private String name;
    @ManyToOne
    @JoinColumn(name = "connection_id")
    private ConnectionConfig connection;

    public MigrationScript(String version, String description, String upScript, String downScript) {
        this.version = version;
        this.description = description;
        this.upScript = upScript;
        this.downScript = downScript;
    }

    public MigrationScript(String version, String description, String script) {
    }

    public MigrationScript(String part, String part1) {
        this.version = part;
        this.description = part1;
    }
}
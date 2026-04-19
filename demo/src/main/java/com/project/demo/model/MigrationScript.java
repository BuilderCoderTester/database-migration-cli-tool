// MigrationScript.java
package com.project.demo.model;

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

    public MigrationScript(String version, String description, String upScript, String downScript) {
        this.version = version;
        this.description = description;
        this.upScript = upScript;
        this.downScript = downScript;
    }

    public MigrationScript(String version, String description, String script) {
    }
}
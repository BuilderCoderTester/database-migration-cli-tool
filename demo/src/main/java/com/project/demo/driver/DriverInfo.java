package com.project.demo.driver;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DriverInfo {
    private DatabaseType databaseType;

    private String driverClass;

    private String version;

    private Path jarPath;

    private boolean loaded;

    public DriverInfo(DatabaseType databaseType, String s, String s1, Path jar) {
    }
}

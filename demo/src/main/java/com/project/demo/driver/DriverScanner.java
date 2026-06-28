package com.project.demo.driver;


import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class DriverScanner {

    private static final String DRIVER_DIRECTORY = "drivers";

    public List<DriverInfo> scan() throws IOException {

        List<DriverInfo> drivers = new ArrayList<>();

        Path driverFolder = Paths.get(DRIVER_DIRECTORY);

        if (!Files.exists(driverFolder)) {
            Files.createDirectories(driverFolder);
            return drivers;
        }

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(driverFolder, "*.jar")) {

            for (Path jar : stream) {

                DriverInfo info = identifyDriver(jar);

                if (info != null) {
                    drivers.add(info);
                }
            }
        }

        return drivers;
    }

    private DriverInfo identifyDriver(Path jar) {

        String file = jar.getFileName().toString().toLowerCase();

        if (file.contains("postgresql")) {

            return new DriverInfo(
                    DatabaseType.POSTGRESQL,
                    "org.postgresql.Driver",
                    extractVersion(file),
                    jar
            );
        }

        if (file.contains("mysql")) {

            return new DriverInfo(
                    DatabaseType.MYSQL,
                    "com.mysql.cj.jdbc.Driver",
                    extractVersion(file),
                    jar
            );
        }

        if (file.contains("mssql")) {

            return new DriverInfo(
                    DatabaseType.SQLSERVER,
                    "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                    extractVersion(file),
                    jar
            );
        }

        if (file.contains("sqlite")) {

            return new DriverInfo(
                    DatabaseType.SQLITE,
                    "org.sqlite.JDBC",
                    extractVersion(file),
                    jar
            );
        }

        if (file.contains("oracle")) {

            return new DriverInfo(
                    DatabaseType.ORACLE,
                    "oracle.jdbc.OracleDriver",
                    extractVersion(file),
                    jar
            );
        }

        return null;
    }

    private String extractVersion(String file) {

        int firstDigit = -1;

        for (int i = 0; i < file.length(); i++) {

            if (Character.isDigit(file.charAt(i))) {
                firstDigit = i;
                break;
            }
        }

        if (firstDigit == -1)
            return "Unknown";

        return file.substring(firstDigit, file.lastIndexOf(".jar"));
    }

}
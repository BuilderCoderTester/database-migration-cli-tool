package com.project.demo.utility;

public class VersionUtils {

    public static long extract(String version) {

        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }

        // Remove non-digits → V10 → 10
        String numeric = version.replaceAll("\\D", "");

        if (numeric.isEmpty()) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        return Long.parseLong(numeric);
    }
}
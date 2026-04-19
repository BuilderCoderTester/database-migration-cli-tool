package com.project.demo.utility;

import java.util.ArrayList;
import java.util.List;

public class Helper {
    public List<String> splitSql(String sql) {

        List<String> statements = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        boolean inString = false;

        for (char c : sql.toCharArray()) {

            if (c == '\'') {
                inString = !inString;
            }

            if (c == ';' && !inString) {
                statements.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            statements.add(current.toString().trim());
        }

        return statements;
    }

    public int compareVersion(String v1, String v2) {

        long n1 = extractNumericVersion(v1);
        long n2 = extractNumericVersion(v2);

        return Long.compare(n1, n2);
    }

    public long extractNumericVersion(String version) {

        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }

        // Remove all non-digits (e.g., "V1" → "1")
        String numeric = version.replaceAll("\\D", "");

        if (numeric.isEmpty()) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        return Long.parseLong(numeric);
    }
}

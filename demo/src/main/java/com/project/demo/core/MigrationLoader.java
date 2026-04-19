package com.project.demo.core;

import com.project.demo.config.MigrationProperties;
import com.project.demo.model.MigrationScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MigrationLoader {

    private static final Pattern MIGRATION_PATTERN =
            Pattern.compile("V(\\d+)__(.+)\\.sql");

    private static final Pattern VERSIONED_PATTERN =
            Pattern.compile("V(\\d+)__([\\w_]+)\\.sql");

    private static final Pattern REPEATABLE_PATTERN =
            Pattern.compile("R__([\\w_]+)\\.sql");

    @Autowired
    private MigrationProperties properties;

    public List<MigrationScript> loadPendingMigrations(String currentVersion) throws IOException {
        List<MigrationScript> pending = new ArrayList<>();
        List<MigrationScript> versioned = new ArrayList<>();
        List<MigrationScript> repeatables = new ArrayList<>();

        Path path = Paths.get(properties.getPath());  // FIXED: use properties

        if (!Files.exists(path)) {
            Files.createDirectories(path);
            return pending;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.sql")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                Matcher matcher = MIGRATION_PATTERN.matcher(fileName);

                Matcher v = VERSIONED_PATTERN.matcher(fileName);
                Matcher r = REPEATABLE_PATTERN.matcher(fileName);

                if (matcher.matches()) {
                    String version = matcher.group(1);
                    String description = matcher.group(2).replace("_", " ");

                    // Only load migrations newer than current version
                    if (currentVersion == null || version.compareTo(currentVersion) > 0) {
                        String content = Files.readString(file);
                        MigrationScript script = parseScript(version, description, content);
                        script.setFileName(fileName);
                        pending.add(script);
                    }
                }
                if (v.matches()) {
                    String version = "V" + v.group(1);
                    String description = v.group(2).replace("_", " ");

                    if (currentVersion == null ||
                            VersionUtils.extract(version) > VersionUtils.extract(currentVersion)) {

                        MigrationScript s = parseScript(version, description, content);
                        s.setFileName(fileName);
                        s.setRepeatable(false);
                        versioned.add(s);
                    }
                }
                if (r.matches()) {
                    String name = r.group(1);

                    MigrationScript s = parseScript("R__" + name, name, content);
                    s.setFileName(fileName);
                    s.setRepeatable(true);
                    s.setName(name);
                    repeatables.add(s);
                }
            }
        }

        // sort versioned numerically
        versioned.sort(Comparator.comparingLong(m -> VersionUtils.extract(m.getVersion())));

        // repeatables can be sorted by name for determinism
        repeatables.sort(Comparator.comparing(MigrationScript::getName));

        // IMPORTANT: versioned first, then repeatables
        List<MigrationScript> all = new ArrayList<>();
        all.addAll(versioned);
        all.addAll(repeatables);

        pending.sort(Comparator.comparing(MigrationScript::getVersion));
        return pending;
    }

    public MigrationScript loadSpecificVersion(String version) throws IOException {
        Path path = Paths.get(properties.getPath());  // FIXED: use properties

        // Try exact match first
        String exactPattern = String.format("V%s__*.sql", version);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, exactPattern)) {
            for (Path file : stream) {
                String content = Files.readString(file);
                String desc = file.getFileName().toString()
                        .replaceFirst("V\\d+__", "")
                        .replace(".sql", "")
                        .replace("_", " ");
                return parseScript(version, desc, content);
            }
        }
        return null;
    }

    private MigrationScript parseScript(String version, String description, String content) {
        String upScript = content;
        String downScript = null;

        // Support for -- DOWN marker to separate up/down scripts
        int downIndex = content.indexOf("-- DOWN");
        if (downIndex != -1) {
            upScript = content.substring(0, downIndex).trim();
            downScript = content.substring(downIndex + 7).trim();
        }

        return new MigrationScript(version, description, upScript, downScript);
    }

    public void createMigrationFile(String version, String description, String upScript, String downScript)
            throws IOException {
        Path path = Paths.get(properties.getPath());  // FIXED: use properties
        Files.createDirectories(path);

        String fileName = String.format("V%s__%s.sql", version, description.replace(" ", "_"));
        Path filePath = path.resolve(fileName);

        StringBuilder content = new StringBuilder();
        content.append("-- Migration: ").append(description).append("\n");
        content.append("-- Version: ").append(version).append("\n\n");
        content.append(upScript);

        if (downScript != null && !downScript.isEmpty()) {
            content.append("\n\n-- DOWN\n\n");
            content.append(downScript);
        }

        Files.writeString(filePath, content.toString());
    }
}
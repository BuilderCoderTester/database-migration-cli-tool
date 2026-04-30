package com.project.demo.core;

import com.project.demo.config.MigrationProperties;
import com.project.demo.model.MigrationScript;
import com.project.demo.utility.VersionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
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
    private final JdbcTemplate jdbcTemplate;

    public MigrationLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MigrationScript> loadPendingMigrations(String currentVersion) throws IOException {

        List<MigrationScript> versioned = new ArrayList<>();
        List<MigrationScript> repeatables = new ArrayList<>();

        Path path = Paths.get(properties.getPath());

        if (!Files.exists(path)) {
            Files.createDirectories(path);
            return List.of();
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.sql")) {

            for (Path file : stream) {

                String fileName = file.getFileName().toString();
                System.out.println("CURRENT FILE : " + fileName);
                Matcher v = VERSIONED_PATTERN.matcher(fileName);
                Matcher r = REPEATABLE_PATTERN.matcher(fileName);

                String content = Files.readString(file); // ✅ FIXED
                // ========================
                // VERSIONED MIGRATIONS
                // ========================
                if (v.matches()) {

                    String version = "V" + v.group(1);
                    String description = v.group(2).replace("_", " ");
                    System.out.println("VERSION : " + version);
                    System.out.println("DESCRIPTION : " + description);

                    if (currentVersion == null ||
                            VersionUtils.extract(version) > VersionUtils.extract(currentVersion)) {

                        MigrationScript script = parseScript(version, description, content);
                        script.setFileName(fileName);
                        script.setRepeatable(false);

                        versioned.add(script);
                    }
                }

                // ========================
                // REPEATABLE MIGRATIONS
                // ========================
                else if (r.matches()) {

                    String name = r.group(1);

                    MigrationScript script = parseScript("R__" + name, name, content);
                    script.setFileName(fileName);
                    script.setRepeatable(true);
                    script.setName(name);

                    repeatables.add(script);
                }
            }
        }

        // 🔥 sort versioned (numeric safe)
        versioned.sort(Comparator.comparingLong(
                m -> VersionUtils.extract(m.getVersion())
        ));

        // 🔥 sort repeatables
        repeatables.sort(Comparator.comparing(MigrationScript::getName));

        // 🔥 combine
        List<MigrationScript> result = new ArrayList<>();
        result.addAll(versioned);
        result.addAll(repeatables);

        return result;
    }

//    public listloadPendingMigrations

    public MigrationScript loadSpecificVersion(String version) throws IOException {
        Path path = Paths.get(properties.getPath());  // FIXED: use properties
        System.out.println(path);
        // Try exact match first
        String exactPattern = String.format("%s__*.sql", version);
        System.out.println(exactPattern);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, exactPattern)) {
            System.out.println(stream);
            for (Path file : stream) {
                String content = Files.readString(file);
                System.out.println(content);
                String desc = file.getFileName().toString()
                        .replaceFirst("V\\d+__", "")
                        .replace(".sql", "")
                        .replace("_", " ");
                System.out.println(desc);
                return parseScript(version, desc, content);
            }
        }
        return null;
    }

    private MigrationScript parseScript(String version, String description, String content) {
        String upScript = content;
        String downScript = null;
        System.out.println("poiny");
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

    public String listAllPendingMigration() {
        try {
            // 1️⃣ Load all migration files from folder
            List<MigrationScript> allScripts = loadFromFolder();

            // 2️⃣ Load executed versions from DB
            Set<String> executedVersions = loadExecutedVersionsFromDB();

            // 3️⃣ Filter pending scripts
            List<MigrationScript> pending = allScripts.stream()
                    .filter(script -> !executedVersions.contains(script.getVersion()))
                    .toList();

            // 4️⃣ Format output
            if (pending.isEmpty()) {
                return "✅ No pending migrations";
            }

            StringBuilder sb = new StringBuilder("⏳ Pending Migrations:\n");

            for (MigrationScript script : pending) {
                sb.append(String.format(" - %s__%s\n",
                        script.getVersion(),
                        script.getDescription()));
            }

            return sb.toString();

        } catch (Exception e) {
            return "❌ Error fetching pending migrations: " + e.getMessage();
        }
    }

    private List<MigrationScript> loadFromFolder() throws IOException {

        Path path = Paths.get("migrations");
        System.out.println("the path is : "+path);
        if (!Files.exists(path)) {
            return List.of();
        }

        return Files.list(path)
                .filter(p -> p.getFileName().toString().endsWith(".sql"))
                .map(this::parseFileName)
                .sorted(Comparator.comparing(MigrationScript::getVersion))
                .toList();
    }

    private MigrationScript parseFileName(Path file) {

        String name = file.getFileName().toString(); // V1__init.sql

        String[] parts = name.replace(".sql", "").split("__");
        for(String c:parts){
            System.out.println("the scripts : " + c);
        }

        return new MigrationScript(
                parts[0],  // version
                parts[1]   // description
        );
    }

    private Set<String> loadExecutedVersionsFromDB() {

        String sql = "SELECT version FROM schema_history WHERE success = true";

        return new HashSet<>(jdbcTemplate.queryForList(sql, String.class));
    }
}
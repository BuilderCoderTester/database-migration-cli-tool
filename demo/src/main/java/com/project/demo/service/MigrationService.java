package com.project.demo.service;

import com.project.demo.component.ConnectionContext;
import com.project.demo.component.MigrationComponent;
import com.project.demo.component.MigrationEngine;
import com.project.demo.component.MigrationLoader;
import com.project.demo.dto.*;
import com.project.demo.dto.request.MigrationRequest;
import com.project.demo.enumuration.DatabaseOperation;
import com.project.demo.model.ConnectionConfig;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.ConnectionRepo;
import com.project.demo.repository.MigrationRepository;
import com.project.demo.utility.Helper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MigrationService {

    private final Helper helper;
    private final MigrationLoader loader;
    private final MigrationEngine engine;
    private final MigrationLockService migrationLockService;
    private final MigrationRepository repository;
    private final ConnectionContext connectionContext;
    private final ConnectionRepo connectionRepo;
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    private final ConnectionRequest connectionRequest;
    public MigrationService(
            Helper helper,
            ConnectionContext connectionContext,
            MigrationLoader loader, MigrationEngine engine, MigrationLockService migrationLockService, MigrationRepository repository, ConnectionRepo connectionRepo, JdbcTemplate jdbcTemplate,  ConnectionRequest connectionRequest) {
        this.helper = helper;
        this.loader = loader;
        this.engine = engine;
        this.migrationLockService = migrationLockService;
        this.repository = repository;
        this.connectionRepo = connectionRepo;
        this.jdbcTemplate = jdbcTemplate;
        this.connectionRequest = connectionRequest;
        this.connectionContext = connectionContext;
    }

    // RETURNS THE STATUS
    public StatusResponse status(Long connectionId) throws SQLException {

        if (connectionId == null) {
            throw new RuntimeException("No active connection selected");
        }

        var currentOpt = helper.getCurrentVersion(connectionId,connectionRequest.getDatabase());
        String current = currentOpt.orElse("None");

        try {
            List<MigrationScript> pending =
                    loader.loadPendingMigrations(currentOpt.orElse(null), connectionId);

            return new StatusResponse(
                    current,
                    pending.size(),
                    pending
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to load migrations", e);
        }
    }

    public void initialize() {
        engine.initialize();
    }

    public List<MigrationScript> listAllPendingMigration(Long connectionId) throws SQLException {
        Connection connection = activeConnection(connectionContext.getCurrentDatabase());
        return loader.listAllPendingMigration(connectionId,connection);
    }

    //MIGRATE THE MIGRATIONS FILE OR SCRIPTS
    public MigrationResult migrate(MigrationRequest migrationRequest) {

        Long connectionId = migrationRequest.getConnectionId();
        String targetVersion = migrationRequest.getTargetVersion();
        System.out.println("Connection ID at service: " + connectionId);

        StringBuilder lockedBy = null;

        if (connectionId == null) {
            throw new RuntimeException("No active connection selected");
        }

        try {
            //ACQUIRE LOCK
//             lockedBy = new StringBuilder(migrationLockService.acquireLock(connectionId, targetVersion));
//             lockedBy = new StringBuilder(migrationLockService.getHostName());
//            System.out.println("the host at the service : "+lockedBy);

            var currentOpt = helper.getCurrentVersion(connectionId,connectionContext.getCurrentDatabase());
            System.out.println("the currentOPT " + currentOpt);
            List<MigrationScript> pending =
                    loader.loadPendingMigrations(currentOpt.orElse(null), connectionId);

            for(MigrationScript sc :pending){
                System.out.println("Script "+ sc.getVersion());
            }
            if (pending.isEmpty()) {
                return new MigrationResult("✓ No pending migrations", 0, 0);
            }

            int success = 0;
            int failed = 0;
            List<String> applied = new ArrayList<>();

            for (MigrationScript script : pending) {
                migrationRequest.setOperation(detectOperation(script.getDescription()));
                System.out.println("Migration script operation " + migrationRequest.getOperation());
                if (targetVersion != null &&
                        helper.compareVersion(script.getVersion(), targetVersion) > 0) {
                    break;
                }

                try {
                    // 🔥 run migration on correct DB
                    engine.migrateUp(script, connectionId,connectionContext.getCurrentDatabase());
                    success++;
                    applied.add(script.getVersion());

                } catch (Exception e) {

                    failed++;

                    // 🔥 save failure
                    repository.saveFailure(
                            script,
                            connectionId,
                            e
                    );

                    return new MigrationResult(
                            String.format(
                                    "✗ Migration failed at %s\nReason: %s\nApplied: %s",
                                    script.getVersion(),
                                    e.getMessage(),
                                    applied
                            ),
                            success,
                            failed
                    );
                }
            }

            return new MigrationResult(
                    String.format(
                            "✓ Migration complete\nApplied: %s\nSuccess: %d, Failed: %d",
                            applied,
                            success,
                            failed
                    ),
                    success,
                    failed
            );

        } catch (IOException e) {
            return new MigrationResult("✗ Migration failed: " + e.getMessage(), 0, 0);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                migrationLockService.releaseLock(connectionId, lockedBy); // 🔥 scoped unlock
            } catch (Exception ignored) {
            }
        }
    }
    public DatabaseOperation detectOperation(String sql) {
        System.out.println(sql);
        String cleaned = sql.lines()
                .map(String::trim)
                .filter(line ->
                        !line.isBlank() &&
                                !line.startsWith("--"))
                .findFirst()
                .orElse("")
                .toUpperCase();

        if (cleaned.startsWith("CREATE")) {
            return DatabaseOperation.CREATE;
        }

        if (cleaned.startsWith("ALTER")) {
            return DatabaseOperation.ALTER;
        }

        if (cleaned.startsWith("DROP")) {
            return DatabaseOperation.DROP;
        }

        if (cleaned.startsWith("INSERT")) {
            return DatabaseOperation.INSERT;
        }

        if (cleaned.startsWith("UPDATE")) {
            return DatabaseOperation.UPDATE;
        }

        if (cleaned.startsWith("DELETE")) {
            return DatabaseOperation.DELETE;
        }

        return DatabaseOperation.UNKNOWN;
    }
    public String rollback(@Option(description = "Target version") String targetVersion, Long connectionId) {
        try {
            System.out.println("the databse = "+ connectionContext.getCurrentDatabase());
            var currentOpt = helper.getCurrentVersion(connectionId,connectionContext.getCurrentDatabase());
            if (currentOpt.isEmpty()) {
                return "No migrations to rollback";
            }
            System.out.println("atleast coming");
            String current = currentOpt.get();
            MigrationScript script = loader.loadSpecificVersion(current, connectionId);

            if (script == null) {
                return "Could not find migration script for version: " + current;
            }
            System.out.println("the script is " + script.getDownScript());
            if (engine.migrateDown(script,connectionContext.getCurrentDatabase())) {
                System.out.println("yes it is working ");
                return "✓ Rolled back version " + current;
            } else {
                return "✗ Rollback failed";
            }

        } catch (IOException e) {
            return "Rollback error: " + e.getMessage();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String repair() {

        List<Migration> failed = repository.findFailedMigrations();

        if (failed.isEmpty()) {
            return "✓ No failed migrations";
        }

        for (Migration m : failed) {

            // 🔧 mark as not dirty
            repository.clearDirtyFlag(m.getVersion());

            // optional: mark as repaired
            repository.markAsRepaired(m.getVersion());
        }

        return "✓ Repaired " + failed.size() + " failed migrations";
    }

    public List<Migration> history(Long connectionId) throws SQLException {
        List<Migration> migrations = helper.getMigrationHistory(connectionId,connectionContext.getCurrentDatabase());
        // Count of migration history records
        System.out.println("Total migration history count: " + migrations.size());
        String[][] data = new String[migrations.size() + 1][5];
        data[0] = new String[]{"Version", "Description", "Executed At", "Time (ms)", "Status"};

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (int i = 0; i < migrations.size(); i++) {
            Migration m = migrations.get(i);
            data[i + 1] = new String[]{
                    m.getVersion(),
                    m.getDescription(),
                    m.getExecutedAt() != null ? m.getExecutedAt().format(formatter) : "-",
                    m.getExecutionTime() != null ? m.getExecutionTime().toString() : "-",
                    m.isSuccess() ? "✓" : "✗"
            };
        }
        return migrations;
    }

    public String create(
            @Option(required = true, description = "Migration version") String version,
            @Option(required = true, description = "Description") String description,
            @Option(defaultValue = "", description = "Up SQL") String up,
            @Option(defaultValue = "", description = "Down SQL") String down
    ) {
        try {
            loader.createMigrationFile(version, description, up, down);
            return String.format("✓ Created migration V%s__%s.sql", version, description.replace(" ", "_"));
        } catch (IOException e) {
            return "Failed to create migration: " + e.getMessage();
        }
    }

    public String validate(Long connectionId) {
        try {
            List<Migration> history = helper.getMigrationHistory(connectionId,connectionContext.getCurrentDatabase());
            int errors = 0;

            for (Migration migration : history) {
                MigrationScript script = loader.loadSpecificVersion(migration.getVersion(), connectionId);
                if (script != null && !helper.validateChecksum(migration.getVersion(), script.getUpScript())) {
                    errors++;
                    System.out.println("Checksum mismatch: " + migration.getVersion());
                }
            }

            return errors == 0 ? "✓ All migrations validated" : "✗ " + errors + " checksum errors found";
        } catch (IOException | SQLException e) {
            return "Validation error: " + e.getMessage();
        }
    }

    public ConnectionResponse connect(ConnectionRequest request) {

        String adminUrl = "jdbc:postgresql://"
                + request.getHost() + ":"
                + request.getPort() + "/postgres";

        String dbName = request.getDatabase();

        if (!dbName.matches("[a-zA-Z0-9_]+")) {
            return new ConnectionResponse(false, "Invalid database name", null);
        }

        try {
            // 🔹 1. Connect to postgres DB
            DataSource adminDs = DataSourceBuilder.create()
                    .url(adminUrl)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .build();

            // 🔹 2. Create DB (optional)

            try (Connection conn = adminDs.getConnection();
                 Statement stmt = conn.createStatement()) {

                conn.setAutoCommit(true);

                String sql = "CREATE DATABASE \"" + dbName + "\"";
                stmt.executeUpdate(sql);

                System.out.println("Database created: " + dbName);

            } catch (SQLException e) {
                // 42P04 = duplicate_database
                if (!"42P04".equals(e.getSQLState())) {
                    throw e;
                }
                System.out.println("Database already exists: " + dbName);
            }

            // 🔹 3. Test connection to target DB
            String targetUrl = "jdbc:postgresql://"
                    + request.getHost() + ":"
                    + request.getPort() + "/"
                    + dbName;

            DataSource targetDs = DataSourceBuilder.create()
                    .url(targetUrl)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .build();

            try (Connection ignored = targetDs.getConnection()) {
                // success
            }

            // 🔹 4. Save config
            ConnectionConfig config = new ConnectionConfig();
            config.setName(request.getName());
            config.setHost(request.getHost());
            config.setPort(request.getPort());
            config.setDatabase(dbName);
            config.setUsername(request.getUsername());
            config.setPassword(request.getPassword());
            config.setSchema(request.getSchema());
            config.setUrl(targetUrl);

            ConnectionConfig saved = connectionRepo.save(config);

            return new ConnectionResponse(true, "Connection successful", saved.getConnectionId());

        } catch (SQLException e) {
            return new ConnectionResponse(false, e.getMessage(), null);
        }
    }

    public Connection activeConnection(String databaseName) throws SQLException {
        System.out.println(databaseName);
        String sql = """
                    SELECT connection_id FROM connections WHERE database = ?
                """;
        String url = """
                SELECT url from connections where connection_id = ?
                """;
        Long connection_id = jdbcTemplate.queryForObject(sql, Long.class, databaseName);
        connectionContext.setCurrentConnectionId(connection_id);
        connectionContext.setCurrentDatabase(databaseName);
        String dbUrl = jdbcTemplate.queryForObject(url,String.class,connection_id);
        System.out.println("conneciton url " + dbUrl);

        Connection newConnection = DriverManager.getConnection(
                dbUrl,
                "postgres",
                "sigilotech"
        );
        return newConnection;
    }

    public Long getConnectionId() {
        return connectionContext.getCurrentConnectionId();
    }

}

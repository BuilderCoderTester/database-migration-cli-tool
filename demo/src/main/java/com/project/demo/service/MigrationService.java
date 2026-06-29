package com.project.demo.service;

import com.project.demo.component.*;
import com.project.demo.dto.*;
import com.project.demo.dto.request.MigrationRequest;
import com.project.demo.enumuration.DatabaseOperation;
import com.project.demo.model.ConnectionConfig;
import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.ConnectionRepo;
import com.project.demo.repository.MigrationRepository;
import com.project.demo.utility.Helper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class MigrationService {

    private final Helper helper;
    private final MigrationLoader loader;
    private final MigrationEngine engine;
    private final MigrationLockService migrationLockService;
    private final MigrationRepository repository;
    private final ConnectionContext connectionContext;
    private final ConnectionRepo connectionRepo;
    private final JdbcTemplate jdbcTemplate;
    private final SchemaDiffGenerator schemaDiffGenerator;
    @Autowired
    private final ConnectionRequest connectionRequest;

    public MigrationService(
            Helper helper,
            ConnectionContext connectionContext,
            MigrationLoader loader, MigrationEngine engine, MigrationLockService migrationLockService, MigrationRepository repository, ConnectionRepo connectionRepo, JdbcTemplate jdbcTemplate, SchemaDiffGenerator schemaDiffGenerator, ConnectionRequest connectionRequest) {
        this.helper = helper;
        this.loader = loader;
        this.engine = engine;
        this.migrationLockService = migrationLockService;
        this.repository = repository;
        this.connectionRepo = connectionRepo;
        this.jdbcTemplate = jdbcTemplate;
        this.schemaDiffGenerator = schemaDiffGenerator;
        this.connectionRequest = connectionRequest;
        this.connectionContext = connectionContext;
    }

    // RETURNS THE STATUS
    public StatusResponse status(Long connectionId) throws SQLException {

        if (connectionId == null) {
            throw new RuntimeException("No active connection selected");
        }

//        var currentOpt = helper.getCurrentVersion(connectionId, connectionRequest.getDatabase());
//        String current = currentOpt.orElse("None");
        Set<String> executedVersions =
                helper.getExecutedVersions(
                        connectionId,
                        connectionContext.getCurrentDatabase()
                );
        try {
//            List<MigrationScript> pending =
//                    loader.loadPendingMigrations(currentOpt.orElse(null), connectionId);
            List<MigrationScript> pending =
                    loader.loadPendingMigrations(
                            executedVersions,
                            connectionId
                    );
            return new StatusResponse(
                    executedVersions.toString(),
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
        return loader.listAllPendingMigration(connectionId, connection);
    }

    //MIGRATE THE MIGRATIONS FILE OR SCRIPTS
    public MigrationResult migrate(MigrationRequest migrationRequest) throws SQLException {

        // Database validation checking
        Long connectionId = migrationRequest.getConnectionId();
        String targetVersion = migrationRequest.getTargetVersion();
        log.info("Starting migration for connection {}", connectionId);

        String lockedBy = null;

        if (connectionId == null) {
            throw new RuntimeException("No active connection selected");
        }
        Connection connection = activeConnection(connectionContext.getCurrentDatabase());
        connection.setAutoCommit(false);

        try {
            //ACQUIRE LOCK
            if (migrationLockService.isLockStale(connection)) {

                log.warn("Stale migration lock detected for connection {}", connectionId);

                migrationLockService.clearStaleLock(connection);
            }
            lockedBy = migrationLockService.acquireLock(connection, connectionId);
            migrationLockService.updateHeartbeat(connection);
//            var currentOpt = helper.getCurrentVersion(connectionId, connectionContext.getCurrentDatabase());
            Set<String> executedVersions =
                    helper.getExecutedVersions(
                            connectionId,
                            connectionContext.getCurrentDatabase()
                    );

//            System.out.println("the currentOPT " + currentOpt);
//            List<MigrationScript> pending =
//                    loader.loadPendingMigrations(currentOpt.orElse(null), connectionId);
            List<MigrationScript> pending =
                    loader.loadPendingMigrations(
                            executedVersions,
                            connectionId
                    );

            for (MigrationScript sc : pending) {
                log.debug("Pending migration script {}", sc.getVersion());
            }
            if (pending.isEmpty()) {
                return new MigrationResult("✓ No pending migrations", 0, 0);
            }

            int success = 0;
            int failed = 0;
            List<String> applied = new ArrayList<>();

            for (MigrationScript script : pending) {
                migrationRequest.setOperation(detectOperation(script.getDescription()));
                log.debug("Detected operation {} for migration {}", migrationRequest.getOperation(), script.getVersion());
                if (targetVersion != null &&
                        helper.compareVersion(script.getVersion(), targetVersion) > 0) {
                    break;
                }

                try {
                    // 🔥 run migration on correct DB
                    engine.migrateUp(script, connectionId, connectionContext.getCurrentDatabase());
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
                migrationLockService.releaseLock(connection, connectionId, lockedBy); // 🔥 scoped unlock
                connection.commit();
            } catch (Exception e) {
                log.warn("Failed to release migration lock cleanly for connection {}", connectionId, e);
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

    @Transactional
    public String rollback(String targetVersion, Long connectionId) {

        try {

            String database =
                    connectionContext.getCurrentDatabase();
            System.out.println("hte database is rollback " + database);
            List<Migration> history =
                    helper.getMigrationHistory(
                            connectionId,
                            database);
            System.out.println(Arrays.asList(history));
            List<MigrationScript> allScripts =
                    history.stream()
                            .map(migration -> {
                                try {
                                    return loader.loadSpecificVersion(
                                            migration.getVersion(),
                                            connectionId);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .filter(Objects::nonNull)
                            .toList();
            System.out.println("the all script s" + allScripts);
            List<MigrationScript> createScripts =
                    allScripts.stream()
                            .filter(script ->
                                    script.getUpScript()
                                            .trim()
                                            .toUpperCase()
                                            .contains("CREATE TABLE"))
                            .toList();
            System.out.println("the create scrupts are " + createScripts);
            for (MigrationScript createScript : createScripts) {

                String tableName =
                        helper.extractTableName(
                                createScript.getUpScript());

                System.out.println(
                        "Processing table : " + tableName);

                List<MigrationScript> dependentScripts =
                        allScripts.stream()
                                .filter(script -> script != createScript)
                                .filter(script -> {

                                    String table =
                                            helper.extractTableName(
                                                    script.getUpScript());

                                    return table != null
                                            && table.equalsIgnoreCase(tableName);
                                })
                                .toList();

                // rollback dependents first
                for (MigrationScript dependent : dependentScripts) {

                    System.out.println(
                            "Rolling back dependent : "
                                    + dependent.getVersion());

                    boolean success =
                            engine.migrateDown(
                                    dependent,
                                    database);

                    if (!success) {
                        return "Failed rollback of "
                                + dependent.getVersion();
                    }
                }

                // rollback create table last
                System.out.println(
                        "Rolling back create script : "
                                + createScript.getVersion());

                boolean success =
                        engine.migrateDown(
                                createScript,
                                database);

                if (!success) {
                    return "Failed rollback of "
                            + createScript.getVersion();
                }
            }

            return "Rollback completed successfully";

        } catch (Exception e) {

            e.printStackTrace();
            return "Rollback error : "
                    + e.getMessage();
        }
    }


    public List<MigrationScript> mapToMigrationScripts(List<Migration> migrations) {

        return migrations.stream()
                .map(migration -> {

                    MigrationScript script = new MigrationScript();

                    script.setVersion(migration.getVersion());
                    script.setDescription(migration.getDescription());

                    // The migration table only stores the UP script
                    script.setUpScript(migration.getScript());

                    script.setRepeatable(migration.isRepeatable());
                    script.setName(migration.getName());
                    script.setConnection(migration.getConnection());

                    return script;
                })
                .toList();
    }

    public String repair(long connectionId, String versionId) throws SQLException, IOException {
        Connection conn = activeConnection(connectionContext.getCurrentDatabase());
        System.out.println("Migration script version " + versionId + "start finding .");
        MigrationScript script = repository.findFailedMigrations(versionId, connectionId);
        System.out.println("Migration script version " + versionId + "end finding .");

        System.out.println("Migration script version " + versionId + "start repairing .");
        repository.markAsRepaired(versionId, script, conn, connectionContext.getCurrentDatabase(), connectionId);

        return "✓ Repaired " + versionId + " failed migrations";
    }

    public List<Migration> history(Long connectionId) throws SQLException {
        List<Migration> migrations = helper.getMigrationHistory(connectionId, connectionContext.getCurrentDatabase());
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

//    public String create(
//            @Option(required = true, description = "Migration version") String version,
//            @Option(required = true, description = "Description") String description,
//            @Option(defaultValue = "", description = "Up SQL") String up,
//            @Option(defaultValue = "", description = "Down SQL") String down
//    ) {
//        try {
//            loader.createMigrationFile( description, up, down);
//            return String.format("✓ Created migration V%s__%s.sql", version, description.replace(" ", "_"));
//        } catch (IOException e) {
//            return "Failed to create migration: " + e.getMessage();
//        }
//    }

    public String validate(Long connectionId, String versionId) {
        try {
            System.out.println("reach point for validate -1");
            MigrationScript script =
                    loader.loadSpecificVersion(versionId, connectionId);

            if (script == null) {
                return "Migration " + versionId + " not found";
            }

            boolean valid =
                    helper.validateChecksum(
                            versionId,
                            script.getUpScript());

            return valid
                    ? "✓ Migration " + versionId + " is valid"
                    : "✗ Checksum mismatch for " + versionId;

        } catch (IOException e) {
            return "Validation error: " + e.getMessage();
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
        String dbUrl = jdbcTemplate.queryForObject(url, String.class, connection_id);
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


    public List<String> getTables(Long connectionId) throws SQLException {
        Connection conn = activeConnection(connectionContext.getCurrentDatabase());

        List<String> tables = new ArrayList<>();
        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                ORDER BY table_name
                """;

        try (
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {
                tables.add(rs.getString("table_name"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.println(Arrays.asList(tables));
        return tables;
    }

//    public TableInfoDTO getTableInfo(Long connectionId, String tableName) throws SQLException {
//
//        System.out.println("=== getTableInfo START ===");
//        System.out.println("Connection Id: " + connectionId);
//        System.out.println("Table Name: " + tableName);
//
//        Connection connection =
//                activeConnection(connectionContext.getCurrentDatabase());
//
//        System.out.println("Connection obtained: " + (connection != null));
//
//        try {
//
//            // Row count
//            Long rowCount = 0L;
//
//            String countSql = "SELECT COUNT(*) FROM " + tableName;
//
//            System.out.println("Executing Count Query: " + countSql);
//
//            try (PreparedStatement stmt = connection.prepareStatement(countSql);
//                 ResultSet rs = stmt.executeQuery()) {
//
//                if (rs.next()) {
//                    rowCount = rs.getLong(1);
//                    System.out.println("Row Count: " + rowCount);
//                } else {
//                    System.out.println("No result returned from COUNT query");
//                }
//            }
//
//            // Column information
//            List<ColumnInfoDTO> columns = new ArrayList<>();
//
//            String columnSql = """
//                    SELECT
//                        column_name,
//                        data_type,
//                        is_nullable
//                    FROM information_schema.columns
//                    WHERE table_schema = 'public'
//                      AND table_name = ?
//                    ORDER BY ordinal_position
//                    """;
//
//            System.out.println("Executing Column Query for table: " + tableName);
//
//            try (PreparedStatement stmt =
//                         connection.prepareStatement(columnSql)) {
//
//                stmt.setString(1, tableName);
//
//                try (ResultSet rs = stmt.executeQuery()) {
//
//                    int columnCounter = 0;
//
//                    while (rs.next()) {
//
//                        String columnName = rs.getString("column_name");
//                        String dataType = rs.getString("data_type");
//                        String nullable = rs.getString("is_nullable");
//
//                        System.out.println(
//                                "Column Found -> Name: " + columnName +
//                                        ", Type: " + dataType +
//                                        ", Nullable: " + nullable
//                        );
//
//                        columns.add(
//                                new ColumnInfoDTO(
//                                        columnName,
//                                        dataType,
//                                        "YES".equals(nullable),
//                                        false
//                                )
//                        );
//
//                        columnCounter++;
//                    }
//
//                    System.out.println("Total Columns Found: " + columnCounter);
//                }
//            }
//
//            System.out.println("Preparing DTO...");
//            System.out.println("Table Name: " + tableName);
//            System.out.println("Row Count: " + rowCount);
//            System.out.println("Column Count: " + columns.size());
//
//            TableInfoDTO dto = new TableInfoDTO(
//                    tableName,
//                    "public",
//                    rowCount,
//                    columns.size(),
//                    columns
//            );
//
//            System.out.println("DTO Created Successfully");
//            System.out.println(dto);
//            System.out.println("=== getTableInfo END ===");
//
//            return dto;
//
//        } catch (SQLException e) {
//
//            System.out.println("ERROR OCCURRED");
//            System.out.println("Message: " + e.getMessage());
//            e.printStackTrace();
//
//            throw new RuntimeException(
//                    "Failed to load table information for " + tableName,
//                    e
//            );
//        }
//    }

    public void delete(long connectionId, String versionId) throws SQLException {
        String sql = "DELETE FROM sub_migration WHERE version = ? AND connection_id = ?";

        try (Connection connection = activeConnection(connectionContext.getCurrentDatabase());
             PreparedStatement statement = connection.prepareStatement(sql)) {
            System.out.println("Version = " + versionId);
            System.out.println("Connection Id = " + connectionId);
            statement.setString(1, versionId);
            statement.setLong(2, connectionId);

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected == 0) {
                throw new RuntimeException("Migration not found for version: " + versionId);
            }
        }
    }


    public MigrationResult migrateUpdatedScript(MigrationRequest request, String version) throws IOException {
        System.out.println("reach point migrate update -1");
        long connectionId = request.getConnectionId();
        MigrationScript newSCript = loader.loadSpecificVersion(version, connectionId);
        try {
            List<MigrationScript> scripts =
                    loader.loadAllRelatedScript(newSCript, connectionId);

            MigrationScript oldScript = loader.getActualScript(scripts.get(0),connectionId);
            System.out.println("the old script "+ oldScript);
            String sql = schemaDiffGenerator.generateDiff(oldScript.getUpScript(),newSCript.getUpScript());
            System.out.println("the alter scirpt " + sql);
            Connection conn = activeConnection(connectionContext.getCurrentDatabase());
            PreparedStatement statement = conn.prepareStatement(sql);
            int affectedRows = statement.executeUpdate();

            System.out.println("Migration executed successfully");
            System.out.println("Affected rows: " + affectedRows);
//            System.out.println(scripts);
//            System.out.println("Loaded scripts count: " + scripts.size());
//
//            for (MigrationScript script : scripts) {
//                System.out.println("Description: " + script.getDescription());
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new MigrationResult(
                "✓ Migration complete\nApplied: %s\nSuccess: %d, Failed: %d",
                0,
                0);
    }
}

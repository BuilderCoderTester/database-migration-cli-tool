package com.project.demo.service;

import com.project.demo.component.ConnectionContext;
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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

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
            MigrationLoader loader,
            MigrationEngine engine,
            MigrationLockService migrationLockService,
            MigrationRepository repository,
            ConnectionRepo connectionRepo,
            JdbcTemplate jdbcTemplate,
            ConnectionRequest connectionRequest
    ) {

        this.helper = helper;

        this.loader = loader;

        this.engine = engine;

        this.migrationLockService =
                migrationLockService;

        this.repository = repository;

        this.connectionRepo = connectionRepo;

        this.jdbcTemplate = jdbcTemplate;

        this.connectionRequest =
                connectionRequest;

        this.connectionContext =
                connectionContext;
    }

    // =====================================================
    // STATUS
    // =====================================================

    public StatusResponse status(
            Long connectionId
    ) throws SQLException {

        if (connectionId == null) {

            throw new RuntimeException(
                    "No active connection selected"
            );
        }

        var currentOpt =
                helper.getCurrentVersion(
                        connectionId,
                        connectionRequest.getDatabase()
                );

        String current =
                currentOpt.orElse("None");

        try {

            List<MigrationScript> pending =
                    loader.loadPendingMigrations(
                            currentOpt.orElse(null),
                            connectionId
                    );

            return new StatusResponse(
                    current,
                    pending.size(),
                    pending
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to load migrations",
                    e
            );
        }
    }

    // =====================================================
    // INITIALIZE
    // =====================================================

    public void initialize() {

        engine.initialize();
    }

    // =====================================================
    // LIST PENDING
    // =====================================================

    public List<MigrationScript>
    listAllPendingMigration(
            Long connectionId
    ) throws SQLException {

        Connection connection =
                activeConnection(
                        connectionContext
                                .getCurrentDatabase()
                );

        return loader.listAllPendingMigration(
                connectionId,
                connection
        );
    }

    // =====================================================
    // MIGRATE
    // =====================================================

    public MigrationResult migrate(
            MigrationRequest migrationRequest
    ) throws SQLException {

        Long connectionId =
                migrationRequest.getConnectionId();

        String targetVersion =
                migrationRequest.getTargetVersion();

        System.out.println(
                "Connection ID at service: "
                        + connectionId
        );

        String lockedBy = null;

        if (connectionId == null) {

            throw new RuntimeException(
                    "No active connection selected"
            );
        }

        Connection connection =
                activeConnection(
                        connectionContext
                                .getCurrentDatabase()
                );

        connection.setAutoCommit(false);

        try {

            // =========================================
            // STALE LOCK CHECK
            // =========================================

            if (
                    migrationLockService
                            .isLockStale(
                                    connection,
                                    connectionId
                            )
            ) {

                System.out.println(
                        "WARNING: stale migration lock detected"
                );

                migrationLockService
                        .clearStaleLock(
                                connection,
                                connectionId
                        );
            }

            // =========================================
            // ACQUIRE LOCK
            // =========================================

            lockedBy =
                    migrationLockService
                            .acquireLock(
                                    connection,
                                    connectionId
                            );

            migrationLockService
                    .updateHeartbeat(
                            connection,
                            connectionId
                    );

            // =========================================
            // CURRENT VERSION
            // =========================================

            var currentOpt =
                    helper.getCurrentVersion(
                            connectionId,
                            connectionContext
                                    .getCurrentDatabase()
                    );

            System.out.println(
                    "Current version = "
                            + currentOpt
            );

            List<MigrationScript> pending =
                    loader.loadPendingMigrations(
                            currentOpt.orElse(null),
                            connectionId
                    );

            for (MigrationScript sc : pending) {

                System.out.println(
                        "Script "
                                + sc.getVersion()
                );
            }

            if (pending.isEmpty()) {

                return new MigrationResult(
                        "✓ No pending migrations",
                        0,
                        0
                );
            }

            int success = 0;

            int failed = 0;

            List<String> applied =
                    new ArrayList<>();

            // =========================================
            // RUN MIGRATIONS
            // =========================================

            for (MigrationScript script : pending) {

                migrationRequest.setOperation(
                        detectOperation(
                                script.getDescription()
                        )
                );

                System.out.println(
                        "Migration operation = "
                                + migrationRequest
                                .getOperation()
                );

                if (
                        targetVersion != null
                                &&
                                helper.compareVersion(
                                        script.getVersion(),
                                        targetVersion
                                ) > 0
                ) {

                    break;
                }

                try {

                    engine.migrateUp(
                            script,
                            connectionId,
                            connectionContext
                                    .getCurrentDatabase()
                    );

                    success++;

                    applied.add(
                            script.getVersion()
                    );

                } catch (Exception e) {

                    failed++;

                    repository.saveFailure(
                            script,
                            connectionId,
                            e
                    );

                    return new MigrationResult(
                            String.format(
                                    """
                                    ✗ Migration failed at %s
                                    Reason: %s
                                    Applied: %s
                                    """,
                                    script.getVersion(),
                                    e.getMessage(),
                                    applied
                            ),
                            success,
                            failed
                    );
                }
            }

            connection.commit();

            return new MigrationResult(
                    String.format(
                            """
                            ✓ Migration complete
                            Applied: %s
                            Success: %d
                            Failed: %d
                            """,
                            applied,
                            success,
                            failed
                    ),
                    success,
                    failed
            );

        } catch (IOException e) {

            connection.rollback();

            return new MigrationResult(
                    "✗ Migration failed: "
                            + e.getMessage(),
                    0,
                    0
            );

        } catch (Exception e) {

            connection.rollback();

            throw new RuntimeException(e);

        } finally {

            try {

                migrationLockService
                        .releaseLock(
                                connection,
                                connectionId,
                                lockedBy
                        );

                connection.close();

            } catch (Exception ignored) {
            }
        }
    }

    // =====================================================
    // DETECT OPERATION
    // =====================================================

    public DatabaseOperation detectOperation(
            String sql
    ) {

        String cleaned =
                sql.lines()
                        .map(String::trim)
                        .filter(line ->
                                !line.isBlank()
                                        &&
                                        !line.startsWith("--")
                        )
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

    // =====================================================
    // ROLLBACK
    // =====================================================

    public String rollback(
            @Option(description =
                    "Target version")
            String targetVersion,

            Long connectionId
    ) {

        try {

            var currentOpt =
                    helper.getCurrentVersion(
                            connectionId,
                            connectionContext
                                    .getCurrentDatabase()
                    );

            if (currentOpt.isEmpty()) {

                return "No migrations to rollback";
            }

            String current =
                    currentOpt.get();

            MigrationScript script =
                    loader.loadSpecificVersion(
                            current,
                            connectionId
                    );

            if (script == null) {

                return
                        "Could not find migration script for version: "
                                + current;
            }

            if (
                    engine.migrateDown(
                            script,
                            connectionContext
                                    .getCurrentDatabase()
                    )
            ) {

                return
                        "✓ Rolled back version "
                                + current;

            } else {

                return "✗ Rollback failed";
            }

        } catch (IOException e) {

            return "Rollback error: "
                    + e.getMessage();

        } catch (SQLException e) {

            throw new RuntimeException(e);
        }
    }

    // =====================================================
    // REPAIR
    // =====================================================

    public String repair() {

        List<Migration> failed =
                repository.findFailedMigrations();

        if (failed.isEmpty()) {

            return "✓ No failed migrations";
        }

        for (Migration m : failed) {

            repository.clearDirtyFlag(
                    m.getVersion()
            );

            repository.markAsRepaired(
                    m.getVersion()
            );
        }

        return "✓ Repaired "
                + failed.size()
                + " failed migrations";
    }

    // =====================================================
    // HISTORY
    // =====================================================

    public List<Migration> history(
            Long connectionId
    ) throws SQLException {

        List<Migration> migrations =
                helper.getMigrationHistory(
                        connectionId,
                        connectionContext
                                .getCurrentDatabase()
                );

        System.out.println(
                "Total migration history count: "
                        + migrations.size()
        );

        return migrations;
    }

    // =====================================================
    // CREATE
    // =====================================================

    public String create(
            String version,
            String description,
            String up,
            String down
    ) {

        try {

            loader.createMigrationFile(
                    version,
                    description,
                    up,
                    down
            );

            return String.format(
                    "✓ Created migration V%s__%s.sql",
                    version,
                    description.replace(" ", "_")
            );

        } catch (IOException e) {

            return
                    "Failed to create migration: "
                            + e.getMessage();
        }
    }

    // =====================================================
    // VALIDATE
    // =====================================================

    public String validate(
            Long connectionId
    ) {

        try {

            List<Migration> history =
                    helper.getMigrationHistory(
                            connectionId,
                            connectionContext
                                    .getCurrentDatabase()
                    );

            int errors = 0;

            for (Migration migration : history) {

                MigrationScript script =
                        loader.loadSpecificVersion(
                                migration.getVersion(),
                                connectionId
                        );

                if (
                        script != null
                                &&
                                !helper.validateChecksum(
                                        migration.getVersion(),
                                        script.getUpScript()
                                )
                ) {

                    errors++;

                    System.out.println(
                            "Checksum mismatch: "
                                    + migration.getVersion()
                    );
                }
            }

            return errors == 0
                    ? "✓ All migrations validated"
                    : "✗ " + errors
                    + " checksum errors found";

        } catch (Exception e) {

            return "Validation error: "
                    + e.getMessage();
        }
    }

    // =====================================================
    // CONNECT
    // =====================================================

    public ConnectionResponse connect(
            ConnectionRequest request
    ) {

        String adminUrl =
                "jdbc:postgresql://"
                        + request.getHost()
                        + ":"
                        + request.getPort()
                        + "/postgres";

        String dbName =
                request.getDatabase();

        try {

            DataSource adminDs =
                    DataSourceBuilder.create()
                            .url(adminUrl)
                            .username(
                                    request.getUsername()
                            )
                            .password(
                                    request.getPassword()
                            )
                            .build();

            try (
                    Connection conn =
                            adminDs.getConnection();

                    Statement stmt =
                            conn.createStatement()
            ) {

                conn.setAutoCommit(true);

                String sql =
                        "CREATE DATABASE \""
                                + dbName
                                + "\"";

                stmt.executeUpdate(sql);

            } catch (SQLException e) {

                if (
                        !"42P04".equals(
                                e.getSQLState()
                        )
                ) {

                    throw e;
                }
            }

            String targetUrl =
                    "jdbc:postgresql://"
                            + request.getHost()
                            + ":"
                            + request.getPort()
                            + "/"
                            + dbName;

            DataSource targetDs =
                    DataSourceBuilder.create()
                            .url(targetUrl)
                            .username(
                                    request.getUsername()
                            )
                            .password(
                                    request.getPassword()
                            )
                            .build();

            try (
                    Connection ignored =
                            targetDs.getConnection()
            ) {
            }

            ConnectionConfig config =
                    new ConnectionConfig();

            config.setName(
                    request.getName()
            );

            config.setHost(
                    request.getHost()
            );

            config.setPort(
                    request.getPort()
            );

            config.setDatabase(
                    dbName
            );

            config.setUsername(
                    request.getUsername()
            );

            config.setPassword(
                    request.getPassword()
            );

            config.setSchema(
                    request.getSchema()
            );

            config.setUrl(targetUrl);

            ConnectionConfig saved =
                    connectionRepo.save(config);

            return new ConnectionResponse(
                    true,
                    "Connection successful",
                    saved.getConnectionId()
            );

        } catch (SQLException e) {

            return new ConnectionResponse(
                    false,
                    e.getMessage(),
                    null
            );
        }
    }

    // =====================================================
    // ACTIVE CONNECTION
    // =====================================================

    public Connection activeConnection(
            String databaseName
    ) throws SQLException {

        String sql =
                """
                SELECT connection_id
                FROM connections
                WHERE database = ?
                """;

        String url =
                """
                SELECT url
                FROM connections
                WHERE connection_id = ?
                """;

        Long connectionId =
                jdbcTemplate.queryForObject(
                        sql,
                        Long.class,
                        databaseName
                );

        connectionContext
                .setCurrentConnectionId(
                        connectionId
                );

        connectionContext
                .setCurrentDatabase(
                        databaseName
                );

        String dbUrl =
                jdbcTemplate.queryForObject(
                        url,
                        String.class,
                        connectionId
                );

        Connection newConnection =
                DriverManager.getConnection(
                        dbUrl,
                        "postgres",
                        "sigilotech"
                );

        return newConnection;
    }

    // =====================================================
    // GET CONNECTION ID
    // =====================================================

    public Long getConnectionId() {

        return connectionContext
                .getCurrentConnectionId();
    }

    // =====================================================
    // GET TABLES
    // =====================================================

    public List<String> getAllTables(
            Long connectionId
    ) throws Exception {

        Connection connection =
                activeConnection(
                        connectionContext
                                .getCurrentDatabase()
                );

        List<String> tables =
                new ArrayList<>();

        DatabaseMetaData metaData =
                connection.getMetaData();

        ResultSet rs =
                metaData.getTables(
                        null,
                        "public",
                        "%",
                        new String[]{"TABLE"}
                );

        while (rs.next()) {

            tables.add(
                    rs.getString("TABLE_NAME")
            );
        }

        return tables;
    }
    public List<Map<String, Object>> getTableData(
        String tableName,
        Long connectionId
) throws Exception {

    Connection connection =
            activeConnection(
                    connectionContext
                            .getCurrentDatabase()
            );

    List<Map<String, Object>> rows =
            new ArrayList<>();

    String sql =
            "SELECT * FROM " + tableName;

    try (
            Statement stmt =
                    connection.createStatement();

            ResultSet rs =
                    stmt.executeQuery(sql)
    ) {

        ResultSetMetaData metaData =
                rs.getMetaData();

        int columnCount =
                metaData.getColumnCount();

        while (rs.next()) {

            Map<String, Object> row =
                    new HashMap<>();

            for (
                    int i = 1;
                    i <= columnCount;
                    i++
            ) {

                row.put(
                        metaData.getColumnName(i),
                        rs.getObject(i)
                );
            }

            rows.add(row);
        }
    }

    return rows;
}
}
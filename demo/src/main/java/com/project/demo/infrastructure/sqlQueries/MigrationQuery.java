package com.project.demo.infrastructure.sqlQueries;

public class MigrationQuery {

    public static final String GET_MIGRATION_SCRIPT_DETAILS = """
            SELECT
                version,
                description,
                script,
                checksum,
                executed_at,
                execution_time,
                success,
                error_message,
                error_stack_trace,
                retry_count,
                dirty,
                repeatable,
                name,
                connection_id
            FROM sub_migration
            WHERE connection_id = ?
              AND version = ?
            """;

    public static final String GET_PENDING_MIGRATION_SCRIPT = """
             SELECT\s
                  version,
                 description,
                 script,
                 checksum,
                 executed_at,
                 execution_time,
                 success,
                 error_message,
                 error_stack_trace,
                 retry_count,
                 dirty,
                 repeatable,
                 name,
                 connection_id
             FROM sub_migration
             WHERE connection_id = ?
             AND success = FALSE;
            \s""";
    public static final String CREATE_TABLE_SUB_CONNECTION = """
            CREATE TABLE IF NOT EXISTS sub_connections (
                connection_id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255),
                host VARCHAR(255),
                port INTEGER,
                database_name VARCHAR(255),
                username VARCHAR(255),
                password VARCHAR(255),
                schema VARCHAR(255),
                url TEXT
            );
            """;
    public static final String CREATE_SCRIPT_STATUS = """
            CREATE TYPE Status AS ENUM (
                'PENDING',
                'RUNNING',
                'SUCCESS',
                'FAILED'
            );
            """;
    public static final String CREATE_TABLE_SUM_MIGRATION = """
            CREATE TABLE IF NOT EXISTS sub_migration (
                id BIGSERIAL PRIMARY KEY,
                version VARCHAR(50) NOT NULL UNIQUE,
                description VARCHAR(255),
                script TEXT,
                checksum VARCHAR(64),
                created_at TIMESTAMP,
                executed_at TIMESTAMP,
                running_time BIGINT,
                execution_success BOOLEAN DEFAULT FALSE,
                status Status DEFAULT 'PENDING',
                error_message TEXT,
                error_stack_trace TEXT,
                retry_count INT DEFAULT 0,
                dirty BOOLEAN DEFAULT FALSE,
                repeatable BOOLEAN DEFAULT FALSE,
                name VARCHAR(255),
                connection_id BIGINT
            );
            """;

    public static final String CREATE_TABLE_MIGRATION_LOCK = """
            CREATE TABLE IF NOT EXISTS migration_lock (
                connection_id BIGINT PRIMARY KEY,
                locked BOOLEAN NOT NULL DEFAULT FALSE,
                locked_at TIMESTAMP,
                locked_by VARCHAR(255),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                heartbeat_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                last_completed_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            """;

    public static final String SELECT_TABLE = """
            SELECT schemaname, tablename
            FROM pg_catalog.pg_tables
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
            ORDER BY schemaname, tablename
            """;

    public static final String ACQUIRE_LOCK = """
             SELECT connection_id
                FROM migration_lock
                WHERE connection_id = ?
                FOR UPDATE
            """;

    public static final String MARKED_LOCK = """
             UPDATE migration_lock
                SET locked = true,
                    locked_at = CURRENT_TIMESTAMP,
                    locked_by = ?,
                    heartbeat_at = CURRENT_TIMESTAMP
                WHERE connection_id = ?
            """;

    public static final String RELEASED_LOCK = """
            UPDATE migration_lock
                SET locked = false,
                    locked_at = NULL,
                    locked_by = NULL,
                    heartbeat_at = NULL,
                    last_completed_time = CURRENT_TIMESTAMP
                WHERE connection_id = ?
            """;

    public static final String CREATE_TABLE_MIGRATION = """
             CREATE TABLE IF NOT EXISTS migration (
                      id BIGSERIAL PRIMARY KEY,
                \s
                      version VARCHAR(50) NOT NULL UNIQUE,
                      description VARCHAR(255),
                \s
                      script TEXT,
                      checksum VARCHAR(64),
                \s
                      created_at TIMESTAMP,
                      executed_at TIMESTAMP,
                      running_time BIGINT,
                \s
                      success BOOLEAN DEFAULT FALSE,
                      status Status Status DEFAULT 'PENDING',\s
                \s
                      error_message TEXT,
                      error_stack_trace TEXT,
                \s
                      retry_count INT DEFAULT 0,
                \s
                      dirty BOOLEAN DEFAULT FALSE,
                      repeatable BOOLEAN DEFAULT FALSE,
                \s
                      name VARCHAR(255),
                      connection_id BIGINT,  -- ✅ correct type
                \s
                          CONSTRAINT fk_connection
                              FOREIGN KEY (connection_id)
                              REFERENCES connections(connection_id)
                              ON DELETE CASCADE
                  );
            \s""";

    public static final String INSERT_INTO_SUB_MIGRATION = """
            INSERT INTO sub_migration (
                version,
                description,
                script,
                checksum,
                created_at,
                executed_at,
                running_time,
                execution_success,
                status,
                connection_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (version)
            DO UPDATE SET
                executed_at = EXCLUDED.executed_at,
                running_time = EXCLUDED.running_time,
                execution_success = EXCLUDED.execution_success,
                status = EXCLUDED.status
            """;
    public static final String SAVE_DATABASE_CONNECTIONS = """
            INSERT INTO DbConnections (
                name,
                host,
                port,
                database_name,
                username,
                password,
                schema_name
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?
            );
            """;
}

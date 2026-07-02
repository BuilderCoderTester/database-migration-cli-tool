package com.project.demo.sqlQueries;

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
            AND success = FALSE;
            """;
}

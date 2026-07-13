package com.project.demo.modules.migration.validator;

import com.project.demo.enumuration.AlterOperation;
import com.project.demo.enumuration.DatabaseOperation;
import com.project.demo.enumuration.Status;
import com.project.demo.modules.migration.dto.MigrationScriptStatus;
import com.project.demo.modules.migration.dto.dependency.response.DependencyAnalysisResult;
import com.project.demo.modules.migration.model.Dependency;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class DependencyValidator {

    public MigrationScriptStatus validate(
            DependencyAnalysisResult dependencyAnalysisResult,
            Connection conn
    ) throws Exception {

        MigrationScriptStatus status =
                new MigrationScriptStatus(Status.PASSED, "Validation Successful");
        List<Dependency> deps = dependencyAnalysisResult.getDependencies();
        DatabaseOperation operation = dependencyAnalysisResult.getDatabaseOperation();
        for (Dependency dep : deps) {

            switch (dep.getType()) {

                case TABLE:

                    switch (operation) {

                        case CREATE:
                            status = validateCreateTable(dep, conn);
                            break;

                        case ALTER:
                        case DROP:
                        case INSERT:
                        case UPDATE:
                        case DELETE:
                            status = validateExistingTable(dep, conn);
                            break;
                    }

                    break;

                case COLUMN:
                    if (operation == DatabaseOperation.CREATE) {
                        // For CREATE TABLE, nothing to validate per column.
                        // The table existence check is already handled by validateCreateTable().
                        break;
                    }

                    if (operation != DatabaseOperation.ALTER) {
                        break;
                    }

                    AlterOperation alterOperation = dependencyAnalysisResult.getAlterOperation();

                    if (alterOperation == null) {
                        throw new RuntimeException("Missing AlterOperation for ALTER statement.");
                    }

                    switch (dependencyAnalysisResult.getAlterOperation()) {

                        case ADD_COLUMN:
                            validateColumnNotExists(dep, conn);
                            break;

                        case DROP_COLUMN:
                            validateColumnExists(dep, conn);
                            break;

                        case MODIFY_COLUMN:
                            validateColumnExists(dep, conn);
                            break;

                        case RENAME_COLUMN:
                            validateRenameColumn(dep, conn);
                            break;
                    }

                    break;

                case FOREIGN_KEY:
                    validateForeignKey(dep, conn);
                    break;

                case INDEX:

                    switch (operation) {

                        case CREATE:
                            validateCreateIndex(dep, conn);
                            break;

                        case DROP:
                            validateDropIndex(dep, conn);
                            break;
                    }

                    break;

                case INSERT:
                    status = validateInsert(dep, conn);
                    break;

                case UPDATE:
                    status = validateUpdate(dep, conn);
                    break;

                case DELETE:
                    status = validateDelete(dep, conn);
                    break;

                case ALTER:
                    status = validateAlter(dep, conn);
                    break;

                case DROP:
                    status = validateDrop(dep, conn);
                    break;

                case VERSION:
                    continue;

                default:
                    throw new RuntimeException(
                            "Unknown dependency type : " + dep.getType()
                    );
            }

            if (status.getStatus() == Status.FAILURE) {
                return status;
            }
        }

        return status;
    }

    // ===================================================
    // TABLE
    // ===================================================
// ===================================================
// CREATE INDEX
// ===================================================
    private void validateRenameColumn(
            Dependency dep,
            Connection conn) throws SQLException {

        String oldColumn = dep.getOldColumnName();
        String newColumn = dep.getNewColumnName();

        // Old column must exist
        if (!columnExists(conn, dep.getTable(), oldColumn)) {
            throw new RuntimeException(
                    "Column does not exist : " + oldColumn);
        }

        // New column must not already exist
        if (columnExists(conn, dep.getTable(), newColumn)) {
            throw new RuntimeException(
                    "Column already exists : " + newColumn);
        }
    }
    private void validateColumnNotExists(
            Dependency dep,
            Connection conn) throws SQLException {

        if (columnExists(
                conn,
                dep.getTable(),
                dep.getColumnName())) {

            throw new RuntimeException(
                    "Column already exists "
                            + dep.getColumnName()
                            + " in table "
                            + dep.getTable()
            );
        }
    }

    private void validateColumnExists(
            Dependency dep,
            Connection conn) throws SQLException {

        if (!columnExists(
                conn,
                dep.getTable(),
                dep.getColumnName())) {

            throw new RuntimeException(
                    "Missing column "
                            + dep.getColumnName()
                            + " in table "
                            + dep.getTable()
            );
        }
    }

    private void validateCreateIndex(
            Dependency dep,
            Connection conn) throws SQLException {

        if (dep.getTable() == null) {
            throw new RuntimeException("Table not specified for index.");
        }

        if (dep.getIndexName() == null) {
            throw new RuntimeException("Index name not specified.");
        }

        if (indexExists(conn, dep.getTable(), dep.getIndexName())) {

            throw new RuntimeException(
                    "Index already exists : "
                            + dep.getIndexName()
            );
        }
    }
    // ===================================================
// DROP INDEX
// ===================================================

    private void validateDropIndex(
            Dependency dep,
            Connection conn) throws SQLException {

        if (dep.getTable() == null) {
            throw new RuntimeException("Table not specified for index.");
        }

        if (dep.getIndexName() == null) {
            throw new RuntimeException("Index name not specified.");
        }

        if (!indexExists(conn, dep.getTable(), dep.getIndexName())) {

            throw new RuntimeException(
                    "Index does not exist : "
                            + dep.getIndexName()
            );
        }
    }

    private MigrationScriptStatus validateCreateTable(
            Dependency dep,
            Connection conn)
            throws SQLException {

        if (tableExists(conn, dep.getTable())) {

            return new MigrationScriptStatus(
                    Status.FAILURE,
                    dep.getTable(),
                    "Table already exists : " + dep.getTable()
            );
        }

        return new MigrationScriptStatus(
                Status.PASSED,
                dep.getTable(),
                "Table can be created."
        );
    }

    private MigrationScriptStatus validateExistingTable(
            Dependency dep,
            Connection conn)
            throws SQLException {

        if (!tableExists(conn, dep.getTable())) {

            return new MigrationScriptStatus(
                    Status.FAILURE,
                    dep.getTable(),
                    "Missing table : " + dep.getTable()
            );
        }

        return new MigrationScriptStatus(
                Status.PASSED,
                dep.getTable(),
                "Table exists."
        );
    }

    private MigrationScriptStatus validateTable(
            Dependency dep,
            Connection conn) throws SQLException {

        if (!tableExists(conn, dep.getTable())) {

            return new MigrationScriptStatus(
                    Status.FAILURE,
                    dep.getTable(),
                    "Missing table : " + dep.getTable()
            );
        }

        return new MigrationScriptStatus(
                Status.PASSED,
                "Table exists."
        );
    }

    // ===================================================
    // INSERT
    // ===================================================

    private MigrationScriptStatus validateInsert(
            Dependency dep,
            Connection conn) throws SQLException {

        return validateTable(dep, conn);
    }

    // ===================================================
    // UPDATE
    // ===================================================

    private MigrationScriptStatus validateUpdate(
            Dependency dep,
            Connection conn) throws SQLException {

        return validateTable(dep, conn);
    }

    // ===================================================
    // DELETE
    // ===================================================

    private MigrationScriptStatus validateDelete(
            Dependency dep,
            Connection conn) throws SQLException {

        return validateTable(dep, conn);
    }

    // ===================================================
    // ALTER
    // ===================================================

    private MigrationScriptStatus validateAlter(
            Dependency dep,
            Connection conn) throws SQLException {

        return validateTable(dep, conn);
    }

    // ===================================================
    // DROP
    // ===================================================

    private MigrationScriptStatus validateDrop(
            Dependency dep,
            Connection conn) throws SQLException {

        return validateTable(dep, conn);
    }

    // ===================================================
    // COLUMN
    // ===================================================

    private void validateColumn(
            Dependency dep,
            Connection conn) throws SQLException {

        if (dep.getTable() == null) {

            throw new RuntimeException(
                    "Table not specified for column "
                            + dep.getColumnName()
            );
        }

        if (!columnExists(
                conn,
                dep.getTable(),
                dep.getColumnName())) {

            throw new RuntimeException(
                    "Missing column "
                            + dep.getColumnName()
                            + " in table "
                            + dep.getTable()
            );
        }
    }

    // ===================================================
    // FOREIGN KEY
    // ===================================================

    private void validateForeignKey(
            Dependency dep,
            Connection conn) throws SQLException {

        if (!tableExists(conn, dep.getReferenceTable())) {

            throw new RuntimeException(
                    "Missing FK table : "
                            + dep.getReferenceTable()
            );
        }

        if (!columnExists(
                conn,
                dep.getReferenceTable(),
                dep.getReferenceColumn())) {

            throw new RuntimeException(
                    "Missing FK column : "
                            + dep.getReferenceColumn()
            );
        }
    }

    // ===================================================
    // INDEX
    // ===================================================

    private void validateIndex(
            Dependency dep,
            Connection conn) throws SQLException {

        if (!indexExists(
                conn,
                dep.getTable(),
                dep.getColumnName())) {

            throw new RuntimeException(
                    "Missing index : "
                            + dep.getColumnName()
            );
        }
    }

    // ===================================================
    // METADATA
    // ===================================================

    private boolean tableExists(
            Connection conn,
            String table) throws SQLException {

        ResultSet rs =
                conn.getMetaData().getTables(
                        null,
                        null,
                        table,
                        null
                );

        return rs.next();
    }

    private boolean columnExists(
            Connection conn,
            String table,
            String column) throws SQLException {

        ResultSet rs =
                conn.getMetaData().getColumns(
                        null,
                        null,
                        table,
                        column
                );

        return rs.next();
    }

    private boolean indexExists(
            Connection conn,
            String table,
            String index) throws SQLException {

        ResultSet rs =
                conn.getMetaData().getIndexInfo(
                        null,
                        null,
                        table,
                        false,
                        false
                );

        while (rs.next()) {

            String idxName =
                    rs.getString("INDEX_NAME");

            if (index != null &&
                    index.equalsIgnoreCase(idxName)) {

                return true;
            }
        }

        return false;
    }
}
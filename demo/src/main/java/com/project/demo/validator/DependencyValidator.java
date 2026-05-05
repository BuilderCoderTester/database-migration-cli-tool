package com.project.demo.validator;

import com.project.demo.enumuration.DependencyType;
import com.project.demo.model.Dependency;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DependencyValidator {

    public void validate(List<Dependency> deps, Connection conn) throws Exception {

        for (Dependency dep : deps) {

            switch (dep.getType()) {

                case TABLE -> validateTable(dep, conn);

                case COLUMN -> validateColumn(dep, conn);

                case FOREIGN_KEY -> validateForeignKey(dep, conn);

                case INDEX -> validateIndex(dep, conn);

                case VERSION -> {
                    // optional (handled elsewhere usually)
                }

                default -> throw new RuntimeException("Unknown dependency type: " + dep.getType());
            }
        }
    }

    // =========================
    // 🔍 TABLE VALIDATION
    // =========================
    private void validateTable(Dependency dep, Connection conn) throws SQLException {
        if (!tableExists(conn, dep.getTable())) {
            throw new RuntimeException("❌ Missing table: " + dep.getTable());
        }
    }

    // =========================
    // 🔍 COLUMN VALIDATION
    // =========================
    private void validateColumn(Dependency dep, Connection conn) throws SQLException {

        if (dep.getTable() == null) {
            throw new RuntimeException("⚠️ Column validation failed: table not specified for column " + dep.getColumn());
        }

        if (!columnExists(conn, dep.getTable(), dep.getColumn())) {
            throw new RuntimeException(
                    "❌ Missing column: " + dep.getColumn() + " in table " + dep.getTable()
            );
        }
    }

    // =========================
    // 🔍 FOREIGN KEY VALIDATION
    // =========================
    private void validateForeignKey(Dependency dep, Connection conn) throws SQLException {

        if (!tableExists(conn, dep.getReferenceTable())) {
            throw new RuntimeException("❌ Missing FK table: " + dep.getReferenceTable());
        }

        if (!columnExists(conn, dep.getReferenceTable(), dep.getReferenceColumn())) {
            throw new RuntimeException(
                    "❌ Missing FK column: " + dep.getReferenceColumn()
                            + " in table " + dep.getReferenceTable()
            );
        }
    }

    // =========================
    // 🔍 INDEX VALIDATION
    // =========================
    private void validateIndex(Dependency dep, Connection conn) throws SQLException {

        if (!indexExists(conn, dep.getTable(), dep.getColumn())) {
            throw new RuntimeException(
                    "❌ Missing index: " + dep.getColumn()
                            + " on table " + dep.getTable()
            );
        }
    }

    // =========================
    // 🗄️ METADATA METHODS
    // =========================

    private boolean tableExists(Connection conn, String table) throws SQLException {
        ResultSet rs = conn.getMetaData().getTables(null, null, table, null);
        return rs.next();
    }

    private boolean columnExists(Connection conn, String table, String column) throws SQLException {
        ResultSet rs = conn.getMetaData().getColumns(null, null, table, column);
        return rs.next();
    }

    private boolean indexExists(Connection conn, String table, String index) throws SQLException {
        ResultSet rs = conn.getMetaData().getIndexInfo(null, null, table, false, false);

        while (rs.next()) {
            String idxName = rs.getString("INDEX_NAME");
            if (index != null && index.equalsIgnoreCase(idxName)) {
                return true;
            }
        }
        return false;
    }
}
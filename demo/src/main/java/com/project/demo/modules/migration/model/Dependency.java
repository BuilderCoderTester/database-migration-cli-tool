package com.project.demo.modules.migration.model;

import com.project.demo.enumuration.DatabaseOperation;
import com.project.demo.enumuration.DependencyType;
import com.project.demo.modules.migration.dto.schemaRequest.ColumnSchemaDto;
import com.project.demo.modules.migration.dto.schemaRequest.ForeignKeyDTO;
import com.project.demo.modules.migration.dto.schemaRequest.PrimaryKeyDTO;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Dependency {

    /**
     * Type of dependency.
     */
    private DependencyType type;
    private DatabaseOperation operation;
    private String indexName;
    /**
     * Current table.
     */
    private String table;

    /**
     * Complete column information.
     */
    private ColumnSchemaDto column;
    // Existing column

    // NEW
    private ColumnSchemaDto targetColumn;
    /**
     * Primary key information.
     */
    private PrimaryKeyDTO primaryKey;

    /**
     * Foreign key information.
     */
    private ForeignKeyDTO foreignKey;

    /**
     * Used only for VERSION dependency.
     */
    private String version;

    /**
     * Constructor used by ASTDependencyExtractor.
     */
    public Dependency(
            DependencyType type,
            String table,
            ColumnSchemaDto column,
            PrimaryKeyDTO primaryKey,
            ForeignKeyDTO foreignKey) {

        this.type = type;
        this.table = table;
        this.column = column;
        this.primaryKey = primaryKey;
        this.foreignKey = foreignKey;
    }

    /**
     * Returns the referenced table for FK dependencies.
     */
    public String getReferenceTable() {

        if (foreignKey == null) {
            return null;
        }

        return foreignKey.getReferencedTable();
    }

    /**
     * Returns the referenced column for FK dependencies.
     */
    public String getReferenceColumn() {

        if (foreignKey == null) {
            return null;
        }

        return foreignKey.getReferencedColumn();
    }

    /**
     * Convenience method.
     */
    public String getColumnName() {

        if (column == null) {
            return null;
        }

        return column.getColumnName();
    }
    public String getOldColumnName() {
        return column == null ? null : column.getColumnName();
    }

    public String getNewColumnName() {
        return targetColumn == null ? null : targetColumn.getColumnName();
    }
}
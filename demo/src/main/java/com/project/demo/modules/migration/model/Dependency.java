package com.project.demo.modules.migration.model;

import com.project.demo.modules.migration.dto.schemaRequest.ColumnSchemaDto;
import com.project.demo.modules.migration.dto.schemaRequest.ForeignKeyDTO;
import com.project.demo.modules.migration.dto.schemaRequest.PrimaryKeyDTO;
import com.project.demo.enumuration.DependencyType;
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

    /**
     * Current table.
     */
    private String table;

    /**
     * Complete column information.
     */
    private ColumnSchemaDto column;

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

    public String getReferenceTable() {
        return foreignKey != null
                ? foreignKey.getReferencedTable()
                : null;
    }

    public String getReferenceColumn() {
        return foreignKey != null
                ? foreignKey.getReferencedColumn()
                : null;
    }
}
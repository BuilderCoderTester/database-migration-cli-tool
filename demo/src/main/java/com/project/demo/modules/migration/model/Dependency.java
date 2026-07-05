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

    public Dependency(DependencyType dependencyType, String name, Object o, Object o1, Object o2) {
    }

    public String getReferenceTable() {
        return null ;
    }

    public String getReferenceColumn() {
        return  null;
    }
}
package com.project.demo.model;

import com.project.demo.dto.schemaRequest.ColumnSchemaDto;
import com.project.demo.dto.schemaRequest.ForeignKeyDTO;
import com.project.demo.dto.schemaRequest.PrimaryKeyDTO;
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
}
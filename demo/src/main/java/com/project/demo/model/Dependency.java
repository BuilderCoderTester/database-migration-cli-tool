package com.project.demo.model;

import com.project.demo.enumuration.DependencyType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

//@Entity
//@Table(name = "dependency")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Dependency {
    private DependencyType type;
    private String table;
    private String column;
    private String referenceTable;
    private String referenceColumn;
}

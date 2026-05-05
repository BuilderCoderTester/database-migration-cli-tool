package com.project.demo.model;

import com.project.demo.enumuration.DependencyType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//@Entity
//@Table(name = "dependency")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Dependency {
    private DependencyType type;
    private String table;
    private String column;
    private String referenceTable;
    private String referenceColumn;
}

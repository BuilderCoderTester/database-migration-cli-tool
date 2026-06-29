package com.project.demo.BeforeExecutionValidation.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class TableModel {
    private String tableName;

    private Map<String, ColumnModel> columns = new LinkedHashMap<>();
}

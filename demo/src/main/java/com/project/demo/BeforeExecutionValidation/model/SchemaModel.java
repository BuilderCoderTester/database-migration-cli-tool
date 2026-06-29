package com.project.demo.BeforeExecutionValidation.model;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class SchemaModel {
    private Map<String, TableModel> tables = new LinkedHashMap<>();
}

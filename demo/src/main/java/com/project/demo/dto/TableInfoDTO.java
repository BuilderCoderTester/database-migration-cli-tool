package com.project.demo.dto;

import lombok.*;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@ResponseBody
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TableInfoDTO {
    private String tableName;
    private String schemaName;
    private Long rowCount;
    private Integer columnCount;
    private List<ColumnInfoDTO> columns;
    private List<Map<String, Object>> rows;

}

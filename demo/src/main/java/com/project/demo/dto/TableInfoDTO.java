package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@ResponseBody
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TableInfoDTO {
    private String tableName;
    private String schemaName;
    private Long rowCount;
    private Integer columnCount;
    private List<ColumnInfoDTO> columns;

}

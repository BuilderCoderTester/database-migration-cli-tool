package com.project.demo.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.ResponseBody;

@AllArgsConstructor
@NoArgsConstructor
@ResponseBody
@Getter
@Setter
public class ColumnInfoDTO {
    private String columnName;
    private String dataType;
    private boolean nullable;
    private boolean primaryKey;
}

package com.project.demo.dto;


import lombok.*;
import org.springframework.web.bind.annotation.ResponseBody;

@AllArgsConstructor
@NoArgsConstructor
@ResponseBody
@Getter
@Setter
@ToString
public class ColumnInfoDTO {
    private String columnName;
    private String dataType;
    private boolean nullable;
    private boolean primaryKey;
}

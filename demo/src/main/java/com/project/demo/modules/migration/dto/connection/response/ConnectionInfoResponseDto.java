package com.project.demo.modules.migration.dto.connection.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.ResponseBody;

@ResponseBody
@AllArgsConstructor
@Getter
@Setter
public class ConnectionInfoResponseDto {
    private String host;
    private String database;
    private int port;
    private String schemaTable;


}

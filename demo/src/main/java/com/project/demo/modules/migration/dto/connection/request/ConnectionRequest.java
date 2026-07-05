package com.project.demo.modules.migration.dto.connection.request;


import lombok.*;
import org.springframework.stereotype.Component;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Component
@Builder

public class ConnectionRequest {

    private String name;       // optional (UI display)
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String schema;

}

package com.project.demo.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ConnectionRequest {

    private String name;       // optional (UI display)
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String schema;

}

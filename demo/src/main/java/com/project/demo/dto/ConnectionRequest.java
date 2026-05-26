package com.project.demo.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Component
public class ConnectionRequest {

    private String name;       // optional (UI display)
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String schema;

}

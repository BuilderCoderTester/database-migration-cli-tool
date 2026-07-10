package com.project.demo.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DatabaseConfig {

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String jdbcUrl;
    public DatabaseConfig(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public DatabaseConfig(String host,
                          int port,
                          String database,
                          String username,
                          String password, String jdbcUrl) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.jdbcUrl = jdbcUrl;
    }

    public DatabaseConfig() {

    }

    public String getJdbcUrl() {
        return String.format(
                "jdbc:postgresql://%s:%d/%s",
                host,
                port,
                database
        );
    }
}
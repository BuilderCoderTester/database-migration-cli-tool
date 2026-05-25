package com.project.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "connections")
@Getter
@Setter
public class ConnectionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long connectionId;

    private String name;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String schema;
    private String url;

    @Override
    public String toString() {
        return "ConnectionConfig{" +
                "connectionId=" + connectionId +
                ", name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", database='" + database + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", schema='" + schema + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}

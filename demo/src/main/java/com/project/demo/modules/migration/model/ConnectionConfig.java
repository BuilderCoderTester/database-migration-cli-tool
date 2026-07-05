package com.project.demo.modules.migration.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

@Entity
@Table(name = "connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Accessors(chain = true)
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

}

package com.project.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "migration")
@Getter
@Setter
@NoArgsConstructor
public class Migration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String version;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String script;

    private String checksum;

    private LocalDateTime executedAt;

    private Long executionTime;

    private boolean success;

    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String errorStackTrace;

    private int retryCount;

    private boolean dirty;
    private boolean repeatable;
    private String name;

    public Migration(String version, String description, String script) {
        this.version = version;
        this.description = description;
        this.script = script;
    }
}
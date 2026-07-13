package com.project.demo.modules.migration.model;

import com.project.demo.enumuration.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "migration")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Migration {

    @Id
    private String version;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String script;

    private String checksum;

    private LocalDateTime executedAt;

    private Long executionTime;
    private Long runningTime;
    private boolean success;
    private Status status;
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String errorStackTrace;

    private int retryCount;

    private boolean dirty;
    private boolean repeatable;
    private String name;
    @ManyToOne
    @JoinColumn(name = "connection_id")
    private ConnectionConfig connection;

    public Migration(String version, String description, String script) {
        this.version = version;
        this.description = description;
        this.script = script;
    }

    public Migration(String vv004, String insertIntoSing, String date, String s, String applied) {
    }

    public boolean getSuccess() {
        return success;
    }


}
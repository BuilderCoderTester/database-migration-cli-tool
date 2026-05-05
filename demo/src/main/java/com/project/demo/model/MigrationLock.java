package com.project.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "migration_lock")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MigrationLock {

    @Id
    @Column(name = "connection_id")
    private Long connectionId;

    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by")
    private String lockedBy;

}
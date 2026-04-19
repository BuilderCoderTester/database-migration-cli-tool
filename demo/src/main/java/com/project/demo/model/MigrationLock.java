package com.project.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Time;
import java.time.LocalDateTime;

@Entity
@Table(name = "migration_lock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MigrationLock {
    @Id
    private int id;

    private Boolean locked;

    private LocalDateTime lockedAt;

    private String lockedBy;

}

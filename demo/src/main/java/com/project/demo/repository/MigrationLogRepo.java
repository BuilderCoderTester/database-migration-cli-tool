package com.project.demo.repository;

import com.project.demo.model.MigrationLogs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationLogRepo extends JpaRepository<MigrationLogs,Integer> {
}

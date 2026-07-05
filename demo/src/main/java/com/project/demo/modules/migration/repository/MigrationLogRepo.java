package com.project.demo.modules.migration.repository;

import com.project.demo.modules.migration.model.MigrationLogs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationLogRepo extends JpaRepository<MigrationLogs,Integer> {
}

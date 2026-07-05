package com.project.demo.modules.migration.repository;

import com.project.demo.modules.migration.model.ConnectionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface ConnectionRepo extends JpaRepository<ConnectionConfig, Long> {


}

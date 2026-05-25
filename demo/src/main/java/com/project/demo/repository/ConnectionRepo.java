package com.project.demo.repository;

import com.project.demo.model.ConnectionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface ConnectionRepo extends JpaRepository<ConnectionConfig, Long> {


}

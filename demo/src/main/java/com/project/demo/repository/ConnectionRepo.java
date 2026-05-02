package com.project.demo.repository;

import com.project.demo.dto.ApiResponse;
import com.project.demo.dto.ConnectionRequest;
import com.project.demo.model.ConnectionConfig;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.SQLException;

@Repository
public interface ConnectionRepo extends JpaRepository<ConnectionConfig, Long> {


}

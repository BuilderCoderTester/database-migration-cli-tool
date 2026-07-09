package com.project.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DynamicDataSourceConfig {

    @Bean
    public DataSource dataSource() throws Exception {

        ConfigManager manager = new ConfigManager();

        if (!manager.configExists()) {
            throw new IllegalStateException(
                    "Database configuration not found."
            );
        }

        DatabaseConfig db = manager.load();

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(db.getJdbcUrl());
        config.setUsername(db.getUsername());
        config.setPassword(db.getPassword());

        config.setDriverClassName("org.postgresql.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);

        return new HikariDataSource(config);
    }

}
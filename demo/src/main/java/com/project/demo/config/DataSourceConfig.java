package com.project.demo.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {
//
//    @Bean
//    public DataSource dataSource() {
//        RoutingDataSource routing = new RoutingDataSource();
//
//        Map<Object, Object> targets = new HashMap<>();
//
//        targets.put("test_demo", build("jdbc:postgresql://localhost:5432/test_demo"));
//        targets.put("another_db", build("jdbc:postgresql://localhost:5432/another_db"));
//
//        routing.setTargetDataSources(targets);
//        routing.setDefaultTargetDataSource(targets.get("test_demo"));
//
//        routing.afterPropertiesSet(); // 🔥 IMPORTANT
//
//        return routing;
//    }
//
//    private DataSource build(String url) {
//        return DataSourceBuilder.create()
//                .url(url)
//                .username("postgres")
//                .password("password")
//                .driverClassName("org.postgresql.Driver")
//                .build();
//    }
}
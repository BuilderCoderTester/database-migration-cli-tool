package com.project.demo.component;

import com.project.demo.modules.migration.repository.MigrationRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MigrationComponent {

    @Autowired
    private final MigrationRepository migrationRepository;
    @Autowired
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    private final ConnectionContext connectionContext;
//    @Autowired
//    private final MigrationService migrationService;
    /// get current version of the schema or scripts
//    public Optional<String> getCurrentVersion(Long connectionId,String databaseName) throws SQLException {
//        Connection connection =migrationService.activeConnection(databaseName);
//        return migrationRepository.findLastSuccessful(connectionId,connection).map(Migration::getVersion);
//    }
//    public Connection activeConnection(String databaseName) throws SQLException {
//        System.out.println(databaseName);
//        String sql = """
//                    SELECT connection_id FROM connections WHERE database = ?
//                """;
//        String url = """
//                SELECT url from connections where connection_id = ?
//                """;
//        Long connection_id = jdbcTemplate.queryForObject(sql, Long.class, databaseName);
//        connectionContext.setCurrentConnectionId(connection_id);
//        String dbUrl = jdbcTemplate.queryForObject(url,String.class,connection_id);
//        System.out.println("conneciton url " + dbUrl);
//
//        Connection newConnection = DriverManager.getConnection(
//                dbUrl,
//                "postgres",
//                "sigilotech"
//        );
//        return newConnection;
//    }

}

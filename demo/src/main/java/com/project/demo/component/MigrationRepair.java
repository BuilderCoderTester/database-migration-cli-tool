package com.project.demo.component;

import com.project.demo.enumuration.DatabaseOperation;
import com.project.demo.modules.migration.model.MigrationScript;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@AllArgsConstructor
public class MigrationRepair {
    @AllArgsConstructor
    @Getter
    @Setter
    @NoArgsConstructor
    public static class AnalysisResult {
        private String tableName;
        private DatabaseOperation databaseOperation;
    }

    private final MigrationLoader loader;
    private final ConnectionContext connectionContext;
    private final JdbcTemplate jdbcTemplate;
    // Starts the flow
    public MigrationScript migrationRepairFlow(MigrationScript migrationScript,long connectionId) throws Exception {

        AnalysisResult result_1 = versionExtraction(migrationScript);
        System.out.println("Function one run -1");

        AnalysisResult result_2 = sendTheOppositeOperation(result_1);
        System.out.println("Function one run -2");

        MigrationScript script = findTheRequiredScript(result_2,connectionId ,connectionContext.getCurrentDatabase());
        System.out.println("Function one run -3");

        return script;
    }
    // Extract the Table name and the Operation ---- First to be called
    public AnalysisResult versionExtraction(MigrationScript migrationScript) {
        String des = migrationScript.getDescription();

        if (des == null || des.isBlank()) return null;

        des = des.trim();

        String normalizer = des.replaceAll("_", "").trim();
        Pattern createPattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+(\\w+)",
                Pattern.CASE_INSENSITIVE
        );

        Pattern insertPattern = Pattern.compile(
                "INSERT\\s+INTO\\s+(\\w+)",
                Pattern.CASE_INSENSITIVE
        );

        Pattern alterPattern = Pattern.compile(
                "ALTER\\s+TABLE\\s+(\\w+)",
                Pattern.CASE_INSENSITIVE
        );

        Pattern dropPattern = Pattern.compile(
                "DROP\\s+TABLE\\s+(\\w+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher;
        matcher = createPattern.matcher(normalizer);
        if (matcher.find()) {
            return new AnalysisResult(matcher.group(1), DatabaseOperation.CREATE);
        }

        matcher = insertPattern.matcher(normalizer);
        if (matcher.find()) {
            return new AnalysisResult(matcher.group(1), DatabaseOperation.INSERT);
        }

        matcher = alterPattern.matcher(normalizer);

        if (matcher.find()) {

            return new AnalysisResult(
                    matcher.group(1),
                    DatabaseOperation.ALTER
            );
        }

        matcher = dropPattern.matcher(normalizer);

        if (matcher.find()) {

            return new AnalysisResult(
                    matcher.group(1),
                    DatabaseOperation.DROP
            );
        }
        return null;
    }

    //Find the required script W.R.T database operation and table name  ---- 3rd to be called
    public MigrationScript findTheRequiredScript(AnalysisResult analysisResult , long connectionId ,String databaseName) throws Exception {

        String operation = String.valueOf(analysisResult.getDatabaseOperation());
        String tableName = analysisResult.getTableName();

        MigrationScript value = searchTable(operation, tableName ,connectionId,databaseName);

        return value;
    }

    // Search the migration folder for listing pending migration  --- 4th to be called.
    private MigrationScript searchTable(String operation, String tableName , long connectionId , String databaseName) throws Exception {
        List<MigrationScript> pendingScript =
                listAllPendingMigration(connectionId,databaseName);

        try {
            for (MigrationScript script : pendingScript) {
                System.out.println("up script : " + script.getUpScript());
                String description = script.getDescription();
                String formatted = description.replace("_", " ").toLowerCase();
                System.out.println("chicago -1 " + description);
                System.out.println("chicago -1 " + operation);
                if (formatted.contains(operation.toLowerCase())
                        && formatted.contains(tableName.toLowerCase())) {
                    System.out.println("chicago -2 ");

                    return script;
                }
//                else {
//                    return script;
//                }
            }
        } catch (Exception e) {
            throw new RuntimeException("OPERATION IS NOT COMPLETED !!!!");
        }
        return null;
    }

    // get the opposite operation of the current operation ----- 2nd to be called
    private AnalysisResult sendTheOppositeOperation(AnalysisResult result) throws Exception {

        String operation = String.valueOf(result.getDatabaseOperation());
        String requiredOperation = "";

        switch (operation.toUpperCase()) {

            case "INSERT":
                requiredOperation = "CREATE";
                break;

            case "DELETE":
                requiredOperation = "INSERT";
                break;

            case "DROP":
                requiredOperation = "CREATE";
                break;

            case "ALTER":
                requiredOperation = "CREATE";
                break;

            default:
                return null;
        }

        AnalysisResult analysisResult = new AnalysisResult();
        analysisResult.setDatabaseOperation(DatabaseOperation.valueOf(requiredOperation));
        analysisResult.setTableName(result.getTableName());
        return analysisResult;
    }


    //Extra required fucntion
    public List<MigrationScript> listAllPendingMigration(Long connectionId ,String databaseName)
            throws SQLException, IOException {

        Connection connection = activeConnection(databaseName);

        List<MigrationScript> list =
                loader.listAllPendingMigration(connectionId, connection);
        System.out.println("reach -point -4");

        List<MigrationScript> pendingMigration = new ArrayList<>();

        for (MigrationScript script : list) {

            pendingMigration.addAll(
                    loader.loadPendingMigrations(
                            Collections.singleton(script.getVersion()),
                            connectionId
                    )
            );
        }

        return pendingMigration;
    }


    public Connection activeConnection(String databaseName) throws SQLException {
        System.out.println(databaseName);
        String sql = """
                    SELECT connection_id FROM connections WHERE database = ?
                """;
        String url = """
                SELECT url from connections where connection_id = ?
                """;
        Long connection_id = jdbcTemplate.queryForObject(sql, Long.class, databaseName);
        connectionContext.setCurrentConnectionId(connection_id);
        connectionContext.setCurrentDatabase(databaseName);
        String dbUrl = jdbcTemplate.queryForObject(url,String.class,connection_id);
        System.out.println("conneciton url " + dbUrl);

        Connection newConnection = DriverManager.getConnection(
                dbUrl,
                "postgres",
                "sigilotech"
        );
        return newConnection;
    }
}

package com.project.demo.api.desktop.javafx;

import com.project.demo.modules.migration.dto.ColumnInfoDTO;
import com.project.demo.modules.migration.dto.TableInfoDTO;
import com.project.demo.modules.migration.service.MigrationService;
import com.project.demo.modules.migration.service.SchemaIntrospectionService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseTablesController {

    @FXML
    private ListView<String> tablesListView;

    @FXML
    private VBox tableContentPlaceholder;

    @Autowired
    private SchemaIntrospectionService schemaIntrospectionService;
    @Autowired
    private MigrationService migrationService;

    @Autowired
    public DatabaseTablesController(SchemaIntrospectionService schemaIntrospectionService) {
        this.schemaIntrospectionService = schemaIntrospectionService;
    }

    @FXML
    public void initialize() {
        loadTables();

        tablesListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldTable, newTable) -> {
                    if (newTable != null) {
                        loadTableDetails(newTable);
                    }
                });
    }

    @FXML
    public void refreshMethod() {
        loadTables();
    }

    private void loadTables() {
        try {
            Long connectionId = migrationService.getConnectionId();
            List<String> tables = schemaIntrospectionService.getTables(connectionId);

            tablesListView.getItems().setAll(tables);
            tableContentPlaceholder.getChildren().clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTableDetails(String tableName) {
        try {
            Long connectionId = migrationService.getConnectionId();
            TableInfoDTO tableInfo = schemaIntrospectionService.getTableInfo(connectionId, tableName);

            VBox content = createTableInfo(tableInfo);

            // Force the dynamic sub-VBox layout to safely fill the StackPane space constraints
            VBox.setVgrow(content, Priority.ALWAYS);

            tableContentPlaceholder.getChildren().setAll(content);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createTableInfo(TableInfoDTO tableInfo) {
        VBox root = new VBox(15);
        VBox.setVgrow(root, Priority.ALWAYS);
        root.setPadding(new Insets(10, 0, 0, 0));

        // --- TITLE ELEMENTS ---
        Label title = new Label(tableInfo.getTableName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 22px; -fx-text-fill: #03363d;");

        Label rowCountLabel = new Label("Rows : " + tableInfo.getRowCount());
        rowCountLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        // =========================================================
        // SECTION 1: COLUMNS TABLE (STRUCTURE)
        // =========================================================
        Label columnTitle = new Label("Columns");
        columnTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #03363d;");

        TableView<ColumnInfoDTO> columnTable = new TableView<>();
        VBox.setVgrow(columnTable, Priority.ALWAYS);
        columnTable.setPrefHeight(200);

        TableColumn<ColumnInfoDTO, String> nameCol = new TableColumn<>("Column");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("columnName"));
        nameCol.setPrefWidth(150);

        TableColumn<ColumnInfoDTO, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("dataType"));
        typeCol.setPrefWidth(150);

        TableColumn<ColumnInfoDTO, Boolean> nullableCol = new TableColumn<>("Nullable");
        nullableCol.setCellValueFactory(new PropertyValueFactory<>("nullable"));
        nullableCol.setPrefWidth(100);

        columnTable.getColumns().addAll(nameCol, typeCol, nullableCol);
        columnTable.getItems().addAll(tableInfo.getColumns());

        // =========================================================
        // SECTION 2: ROWS TABLE (DATA) - FIXED FOR MAP
        // =========================================================
        Label dataTitle = new Label("Table Data Rows");
        dataTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #03363d;");

        // 1. Change table type to use Map<String, Object> matching your DTO
        TableView<Map<String, Object>> rowsTable = new TableView<>();
        VBox.setVgrow(rowsTable, Priority.ALWAYS);
        rowsTable.setPrefHeight(250);

        // 2. Dynamically build table columns mapping directly to Map keys
        if (tableInfo.getColumns() != null) {
            for (ColumnInfoDTO colInfo : tableInfo.getColumns()) {
                String columnName = colInfo.getColumnName();

                TableColumn<Map<String, Object>, String> dynamicCol = new TableColumn<>(columnName);

                // Fetch value from map using column name as the key
                dynamicCol.setCellValueFactory(cellData -> {
                    Map<String, Object> rowMap = cellData.getValue();
                    if (rowMap != null && rowMap.containsKey(columnName)) {
                        Object value = rowMap.get(columnName);
                        return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "NULL");
                    }
                    return new javafx.beans.property.SimpleStringProperty("");
                });

                dynamicCol.setPrefWidth(120);
                rowsTable.getColumns().add(dynamicCol);
            }
        }

        // 3. FIX: Add the actual row maps data list from your DTO into the table view
        if (tableInfo.getRows() != null) {
            rowsTable.getItems().addAll(tableInfo.getRows());
        }

        // --- PACK EVERYTHING TOGETHER ---
        root.getChildren().addAll(
                title,
                rowCountLabel,
                columnTitle,
                columnTable,
                dataTitle,
                rowsTable
        );

        return root;
    }
}

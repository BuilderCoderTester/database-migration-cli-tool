package com.project.demo.api.rest.javafx;

import com.project.demo.Main;
import com.project.demo.modules.migration.dto.migration.request.ExecuteMigrationRequestDto;
import com.project.demo.modules.migration.dto.migration.request.MigrationRollbackRequestDto;
import com.project.demo.modules.migration.dto.migration.request.MigrationUpdateRequestDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationResultResponseDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationRollbackResponseDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationUpdateResponseDto;
import com.project.demo.modules.migration.dto.response.MigrationDescriptionResponse;
import com.project.demo.modules.migration.dto.response.MigrationScriptCreateResponse;
import com.project.demo.modules.migration.model.MigrationScript;
import com.project.demo.modules.migration.service.MigrationLifecycleService;
import com.project.demo.modules.migration.service.MigrationScriptService;
import com.project.demo.modules.migration.service.MigrationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Component
public class MigrationScriptsController {

    @FXML
    public Label versionLabel;
    @FXML
    public Label descriptionLabel;
    @FXML
    public Label statusLabel;
    @FXML
    public Label typeLabel;
    @FXML
    public TextArea upScriptArea;
    @FXML
    public TextArea downScriptArea;
    @FXML
    private BorderPane scriptPane; // Tied to fx:id="scriptPane"
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private ComboBox<String> typeFilter;
    @FXML
    private ComboBox<String> versionFilter;
    @FXML
    private TextArea scriptPreview;

    @FXML
    private TableView<MigrationDescriptionResponse> migrationTable;
    @FXML
    private TableColumn<MigrationDescriptionResponse, String> versionColumn;

    @FXML
    private TableColumn<MigrationDescriptionResponse, String> descriptionColumn;

    @FXML
    private TableColumn<MigrationDescriptionResponse, String> typeColumn;

    @FXML
    private TableColumn<MigrationDescriptionResponse, String> statusColumn;

    @FXML
    private TableColumn<MigrationDescriptionResponse, String> executedColumn;

    @FXML
    private TableColumn<MigrationDescriptionResponse, String> durationColumn;
    @FXML
    private TableColumn<MigrationDescriptionResponse, Boolean> selectColumn;
    @Autowired
    private MigrationLifecycleService migrationLifecycleService;
    @Autowired
    private MigrationScriptService migrationScriptService;

    @Autowired
    private MigrationService migrationService;

    @FXML
    public void initialize() {
        migrationTable.setEditable(true);

        selectColumn.setCellValueFactory(cellData ->
                cellData.getValue().selectedProperty());

        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));

        selectColumn.setEditable(true);
        versionColumn.setCellValueFactory(new PropertyValueFactory<>("version"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("success"));
        executedColumn.setCellValueFactory(new PropertyValueFactory<>("executed"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("executionTime"));
    }

    @FXML
    public void createNewMigrations(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/CreateMigrationDialog.fxml"));
        loader.setControllerFactory(Main.getContext()::getBean);
        Parent createScript = loader.load();

        CreateMigrationController controller = loader.getController();
        controller.setParentPane(scriptPane);

        scriptPane.getProperties().put("DASHBOARD_VIEW", scriptPane.getCenter());
        scriptPane.setCenter(createScript);
    }

    @FXML
    public void refreshMethod(ActionEvent actionEvent) throws SQLException {
        long connectionId = migrationService.getConnectionId();
        List<MigrationDescriptionResponse> scripts = migrationLifecycleService.history(connectionId);
        for (MigrationDescriptionResponse response : scripts) {
            System.out.println(response);
        }
        migrationTable.getItems().setAll(scripts);
    }

    @FXML
    public void previewMigrationScripts(ActionEvent actionEvent) throws IOException {
        MigrationDescriptionResponse selectedMigration = migrationTable.getItems()
                .stream()
                .filter(MigrationDescriptionResponse::isSelected)
                .findFirst()
                .orElse(null);

        if (selectedMigration == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("Please select a migration.");
            alert.showAndWait();
            return;
        }
        long connectionId = migrationService.getConnectionId();
        String version = selectedMigration.getVersion();

        MigrationScript script = migrationScriptService.viewScript(version, connectionId);
        System.out.println("The selected script is : " + script);

        versionLabel.setText(script.getVersion());
        descriptionLabel.setText(script.getDescription());
        statusLabel.setText("Empty for now");
        typeLabel.setText("Empty for now");
        upScriptArea.setText(script.getUpScript());
        downScriptArea.setText(script.getDownScript());
    }

    @FXML
    public void handleMigrate(ActionEvent actionEvent) throws SQLException {
        MigrationDescriptionResponse selectedMigration = migrationTable.getItems()
                .stream()
                .filter(MigrationDescriptionResponse::isSelected)
                .findFirst()
                .orElse(null);
        if (selectedMigration == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("Please select a migration.");
            alert.showAndWait();
            return;
        }
        long connectionId = migrationService.getConnectionId();
        String version = selectedMigration.getVersion();
        ExecuteMigrationRequestDto requestDto = new ExecuteMigrationRequestDto(connectionId, version);
        MigrationResultResponseDto response = migrationLifecycleService.executeMigrationScriptsByVersion(requestDto);
    }

    public void handleUpdate(ActionEvent actionEvent) {
    }

    public void handleRepair(ActionEvent actionEvent) {
    }
    @FXML
    public void handleRollback(ActionEvent actionEvent) throws IOException {
        MigrationDescriptionResponse selectedMigration = migrationTable.getItems()
                .stream()
                .filter(MigrationDescriptionResponse::isSelected)
                .findFirst()
                .orElse(null);
        if (selectedMigration == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("Please select a migration.");
            alert.showAndWait();
            return;
        }
        long connectionId = migrationService.getConnectionId();
        String version = selectedMigration.getVersion();
        String rollbackType = null;
        MigrationRollbackRequestDto requestDto = new MigrationRollbackRequestDto(version,connectionId,rollbackType);
        MigrationRollbackResponseDto response = migrationLifecycleService.rollbackMigrationScriptByVersion(requestDto);
    }
}
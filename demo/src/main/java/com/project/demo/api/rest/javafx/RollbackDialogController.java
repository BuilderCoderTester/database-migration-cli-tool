package com.project.demo.api.rest.javafx;

import com.project.demo.modules.migration.dto.migration.request.ExecuteMigrationRequestDto;
import com.project.demo.modules.migration.dto.migration.request.MigrationRollbackRequestDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationResultResponseDto;
import com.project.demo.modules.migration.dto.migration.response.MigrationRollbackResponseDto;
import com.project.demo.modules.migration.dto.response.MigrationDescriptionResponse;
import com.project.demo.modules.migration.service.MigrationLifecycleService;
import com.project.demo.modules.migration.service.MigrationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RollbackDialogController {
    @FXML
    public RadioButton autoDetectRadio;
    @FXML

    public RadioButton versionRadio;
    @FXML

    public RadioButton createRadio;
    @FXML
    public RadioButton insertRadio;
    @FXML
    public RadioButton alterRadio;
    @FXML
    public ToggleGroup strategyGroup;

    @Autowired
    private MigrationLifecycleService migrationLifecycleService;

    private MigrationDescriptionResponse selectedMigration;
    private long connectionId;


    public void handleCancel(ActionEvent actionEvent) {
    }

    @FXML
    public void handleExecute(ActionEvent actionEvent) {
        // 1. Identify which radio button strategy is selected
        if (strategyGroup.getSelectedToggle() == null) {
            System.out.println("No rollback strategy selected.");
            return;
        }

        // 2. Route to your specific database rollback logic
        if (autoDetectRadio.isSelected()) {
            executeAutoDetectRollback();
        } else if (versionRadio.isSelected()) {
            executeRollbackByVersion();
        } else if (createRadio.isSelected()) {
            executeRollbackCreate();
        } else if (insertRadio.isSelected()) {
            executeRollbackInsert();
        } else if (alterRadio.isSelected()) {
            executeRollbackAlter();
        }

        // 3. Close the modal dialog window automatically after execution
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.close();
    }

// --- Example Helper Methods for Data Layer Integration ---

    private void executeAutoDetectRollback() {
        System.out.println("Executing: Auto Detect Rollback Strategy");
        // Place database execution code or service calls here
    }

    private void executeRollbackByVersion() {
        System.out.println("Executing: Rollback by Version");
    }

    private void executeRollbackCreate() {
        System.out.println("Executing: Rollback CREATE statements only");
        String version = selectedMigration.getVersion();
        String type = "CREATE";

        MigrationRollbackRequestDto request =
                new MigrationRollbackRequestDto(version, connectionId, type);

        MigrationRollbackResponseDto response =
                migrationLifecycleService.rollbackMigrationScriptByVersion(request);

    }

    private void executeRollbackInsert() {
        System.out.println("Executing: Rollback INSERT statements only");
    }

    private void executeRollbackAlter() {
        System.out.println("Executing: Rollback ALTER statements only");
    }

    public void setRollbackData(long connectionId,
                                MigrationDescriptionResponse selectedMigration) {
        this.connectionId = connectionId;
        this.selectedMigration = selectedMigration;
    }
}

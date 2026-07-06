package com.project.demo.api.rest.javafx;

import com.project.demo.modules.migration.dto.response.MigrationScriptCreateResponse;
import com.project.demo.modules.migration.service.MigrationScriptService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateMigrationController {
    @FXML
    private TextField migrationNameField;
    @FXML
    private TextArea upMigrationArea;
    @FXML
    private TextArea downMigrationArea;

    @Setter
    private BorderPane parentPane;

    @Autowired
    private MigrationScriptService migrationScriptService;

    @FXML
    public void handleCancel(ActionEvent actionEvent) {
        returnToDashboard();
    }

    private void returnToDashboard() {
        if (parentPane != null) {
            Node originalDashboardView = (Node) parentPane.getProperties().get("DASHBOARD_VIEW");
            if (originalDashboardView != null) {
                parentPane.setCenter(originalDashboardView);
            }
        }
    }


    @FXML
    public void handleCreate(ActionEvent actionEvent) {
        String migrationScriptName = migrationNameField.getText();
        System.out.println("Saving: " + migrationScriptName);
        String upScript = upMigrationArea.getText();
        System.out.println("up script: " + upScript);
        String downScript = downMigrationArea.getText();
        System.out.println("down script : " + downScript);

        MigrationScriptCreateResponse response = migrationScriptService.create(null, migrationScriptName, upScript, downScript);
        System.out.println(response.toString());
        returnToDashboard();
    }

}

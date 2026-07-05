package com.project.demo.api.rest.javafx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MigrationScriptsController {

    @FXML
    private BorderPane scriptPane;

    @FXML
    public void initialize() {

    }

    public void createNewMigrations(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/CreateMigrationDialog.fxml"));
        Parent createScript = loader.load();
        scriptPane.setCenter(createScript);
    }
}
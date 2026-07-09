package com.project.demo.setup;

import com.project.demo.Main;
import com.project.demo.config.DatabaseConfig;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SetupController {

    @FXML
    private TextField hostField;

    @FXML
    private TextField portField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private final SetupService setupService = new SetupService();

    @FXML
    public void initialize() {

        hostField.setText("localhost");
        portField.setText("5432");
        usernameField.setText("postgres");

    }

    @FXML
    private void testConnection(ActionEvent event) {

        DatabaseConfig config = new DatabaseConfig();

        config.setHost(hostField.getText().trim());
        config.setPort(Integer.parseInt(portField.getText().trim()));
        config.setDatabase("postgres");
        config.setUsername(usernameField.getText().trim());
        config.setPassword(passwordField.getText());

        if (setupService.testConnection(config)) {

            showInfo("Success", "Connection Successful.");

        } else {

            showError("Connection Failed",
                    "Unable to connect to PostgreSQL.");

        }

    }

    @FXML
    private void continueSetup(ActionEvent event) {

        DatabaseConfig config = new DatabaseConfig();

        config.setHost(hostField.getText().trim());
        config.setPort(Integer.parseInt(portField.getText().trim()));
        config.setDatabase("postgres");
        config.setUsername(usernameField.getText().trim());
        config.setPassword(passwordField.getText());

        try {

            if (!setupService.initialize(config)) {

                showError(
                        "Setup Failed",
                        "Unable to initialize PostgreSQL.");

                return;
            }

            Main.startSpring();

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/Dashboard.fxml"));

            loader.setControllerFactory(Main.getContext()::getBean);

            Parent root = loader.load();

            Stage stage = (Stage) hostField.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.setTitle("Migration Tool");

        } catch (Exception ex) {

            ex.printStackTrace();

            showError(
                    "Error",
                    ex.getMessage());

        }

    }

    private void showInfo(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setHeaderText(title);
        alert.setContentText(message);

        alert.showAndWait();

    }

    private void showError(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setHeaderText(title);
        alert.setContentText(message);

        alert.showAndWait();

    }

}
package com.project.demo.api.rest.javafx;

import com.project.demo.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import org.springframework.stereotype.Component;

@Component
public class DashboardController {

    @FXML
    private BorderPane mainPane;

    @FXML
    private TableView<?> pendingTable;

    @FXML
    private TableColumn<?, ?> versionColumn;

    @FXML
    private TableColumn<?, ?> descriptionColumn;

    @FXML
    private TableColumn<?, ?> statusColumn;

    @FXML
    private TableView<?> historyTable;

    @FXML
    public void initialize() {
        // UI only
    }

    @FXML
    private void openConnections() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ConnectionView.fxml"));

            loader.setControllerFactory(Main.getContext()::getBean);

            Parent view = loader.load();

            mainPane.setCenter(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMigrationScriptsClick(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MigrationScripts.fxml"));
            loader.setControllerFactory(Main.getContext()::getBean);
            Parent migrationScriptView = loader.load();

            mainPane.setCenter(migrationScriptView);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handelDatabaseConnection(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DatabaseConnections.fxml"));
            loader.setControllerFactory(Main.getContext()::getBean);
            Parent migrationScriptView = loader.load();

            mainPane.setCenter(migrationScriptView);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handelDatabaseTables(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DatabaseTables.fxml"));
            loader.setControllerFactory(Main.getContext()::getBean);
            Parent migrationScriptView = loader.load();

            mainPane.setCenter(migrationScriptView);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handelActivityLogs(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ActivityLogs.fxml"));
            loader.setControllerFactory(Main.getContext()::getBean);
            Parent migrationScriptView = loader.load();

            mainPane.setCenter(migrationScriptView);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handelRunHsitory(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RunHistory.fxml"));
            loader.setControllerFactory(Main.getContext()::getBean);
            Parent migrationScriptView = loader.load();

            mainPane.setCenter(migrationScriptView);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
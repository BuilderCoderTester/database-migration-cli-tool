package com.project.demo.api.rest.javafx;

import com.project.demo.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

@Component
public class DashboardController {

    @FXML
    private BorderPane mainPane;

    // Cache the original dashboard content view to allow users to navigate "Home"
    @FXML
    private VBox dashboardContent;

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

    private Node homeView;

    @FXML
    public void initialize() {
        // Cache the default dashboard center view right after FXML parsing finishes
        this.homeView = mainPane.getCenter();
    }

    /**
     * Helper method to dynamically load a sub-view into the main panel area.
     * Prevents dry code duplication and cleanly routes Spring beans.
     */
    private void switchView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(Main.getContext()::getBean);
            Parent view = loader.load();

            // Replaces the center zone smoothly
            mainPane.setCenter(view);
        } catch (Exception e) {
            // Replaced generic RuntimeException with logging/printing for cleaner debugging
            System.err.println("Failed to switch view to: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    public void handleDashboardHomeClick(ActionEvent actionEvent) {
        // Safely route the user back to the home view metrics and charts
        if (homeView != null) {
            mainPane.setCenter(homeView);
        }
    }

    @FXML
    public void handleMigrationScriptsClick(ActionEvent actionEvent) {
        switchView("/MigrationScripts.fxml");
    }

    @FXML
    public void handelDatabaseConnection(ActionEvent actionEvent) {
        switchView("/DatabaseConnections.fxml");
    }

    @FXML
    public void handelDatabaseTables(ActionEvent actionEvent) {
        switchView("/DatabaseTables.fxml");
    }

    @FXML
    public void handelActivityLogs(ActionEvent actionEvent) {
        switchView("/ActivityLogs.fxml");
    }

    @FXML
    public void handelRunHsitory(ActionEvent actionEvent) {
        switchView("/RunHistory.fxml");
    }

}
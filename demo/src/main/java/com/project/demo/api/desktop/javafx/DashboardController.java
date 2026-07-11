package com.project.demo.api.desktop.javafx;

import com.project.demo.Main;
import com.project.demo.modules.migration.dto.response.MigrationDescriptionResponse;
import com.project.demo.modules.migration.service.MigrationLifecycleService;
import com.project.demo.modules.migration.service.MigrationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DashboardController {

    @FXML private BorderPane mainPane;
    @FXML private VBox dashboardContent;

    // --- TOP STATS METRIC CARDS ---
    @FXML private Label totalMigrationsLabel;
    @FXML private Label appliedMigrationsLabel;
    @FXML private Label pendingMigrationsLabel;
    @FXML private Label failedMigrationsLabel;

    // --- PENDING MIGRATIONS TABLE ---
    @FXML private TableView<MigrationDescriptionResponse> pendingTable;
    @FXML private TableColumn<MigrationDescriptionResponse, String> versionColumn;
    @FXML private TableColumn<MigrationDescriptionResponse, String> descriptionColumn;
    @FXML private TableColumn<MigrationDescriptionResponse, Boolean> statusColumn;

    // --- RECENT EXECUTION HISTORY TABLE ---
    @FXML private TableView<MigrationDescriptionResponse> historyTable;
    @FXML private TableColumn<MigrationDescriptionResponse, String> historyMigrationColumn;
    @FXML private TableColumn<MigrationDescriptionResponse, LocalDateTime> historyDateColumn;
    @FXML private TableColumn<MigrationDescriptionResponse, Boolean> historyStatusColumn;

    @Autowired private MigrationLifecycleService migrationLifecycleService;
    @Autowired private MigrationService migrationService;

    private final ObservableList<MigrationDescriptionResponse> historyData = FXCollections.observableArrayList();
    private final ObservableList<MigrationDescriptionResponse> pendingData = FXCollections.observableArrayList();
    private Node homeView;

    @FXML
    public void initialize() {
        this.homeView = mainPane.getCenter();

        // 1. Setup Recent Execution History Table Columns
        if (historyTable != null) {
            historyMigrationColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            historyDateColumn.setCellValueFactory(new PropertyValueFactory<>("executedAt"));
            historyStatusColumn.setCellValueFactory(new PropertyValueFactory<>("success"));
            setupStatusCellFactory(historyStatusColumn);
            historyTable.setItems(historyData);
        }

        // 2. Setup Pending Table Columns
        if (pendingTable != null) {
            versionColumn.setCellValueFactory(new PropertyValueFactory<>("version"));
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("success"));
            setupStatusCellFactory(statusColumn);
            pendingTable.setItems(pendingData);
        }

        // Initial fetch on view loading
        refreshDashboardDataOnDemand();
    }

    /**
     * Aggregates card counter metrics and populates tables.
     */
    private void refreshDashboardDataOnDemand() {
        Long connectionId = migrationService.getConnectionId();

        if (connectionId != null) {
            try {
                List<MigrationDescriptionResponse> allHistory = migrationLifecycleService.history(connectionId);

                if (allHistory != null) {
                    // --- CALCULATE CARD STATS ---
                    long totalCount = allHistory.size();

                    // Count how many are applied (success == true)
                    long appliedCount = allHistory.stream().filter(MigrationDescriptionResponse::isSuccess).count();

                    // Count how many failed (executed but success == false)
                    long failedCount = allHistory.stream().filter(r -> r.getExecutedAt() != null && !r.isSuccess()).count();

                    // Count how many are completely pending (not executed yet, meaning executedAt is null)
                    long pendingCount = allHistory.stream().filter(r -> r.getExecutedAt() == null).count();

                    // --- ASSIGN VALUES TO CARD LABELS ---
                    totalMigrationsLabel.setText(String.format("%02d", totalCount));
                    appliedMigrationsLabel.setText(String.format("%02d", appliedCount));
                    pendingMigrationsLabel.setText(String.format("%02d", pendingCount));
                    failedMigrationsLabel.setText(String.format("%02d", failedCount));

                    // --- UPDATE RECENT HISTORY TABLE (Last 3 Hours) ---
                    LocalDateTime cutoffTime = LocalDateTime.now().minusHours(3);
                    historyData.clear();
                    allHistory.stream()
                            .filter(record -> record.getExecutedAt() != null && record.getExecutedAt().isAfter(cutoffTime))
                            .forEach(historyData::add);

                    // --- UPDATE PENDING TABLE (Non-successful or unexecuted scripts) ---
                    pendingData.clear();
                    allHistory.stream()
                            .filter(record -> !record.isSuccess())
                            .forEach(pendingData::add);
                }
            } catch (Exception e) {
                System.err.println("Failed to aggregate dashboard analytics summary counters.");
                e.printStackTrace();
            }
        } else {
            // Safe reset fallback state if no database link is initialized
            resetStatsToZero();
        }
    }

    private void resetStatsToZero() {
        String zeroStr = "00";
        if (totalMigrationsLabel != null) totalMigrationsLabel.setText(zeroStr);
        if (appliedMigrationsLabel != null) appliedMigrationsLabel.setText(zeroStr);
        if (pendingMigrationsLabel != null) pendingMigrationsLabel.setText(zeroStr);
        if (failedMigrationsLabel != null) failedMigrationsLabel.setText(zeroStr);
        historyData.clear();
        pendingData.clear();
    }

    @FXML
    public void handleDashboardHomeClick(ActionEvent actionEvent) {
        if (homeView != null) {
            mainPane.setCenter(homeView);
            refreshDashboardDataOnDemand();
        }
    }

    private void setupStatusCellFactory(TableColumn<MigrationDescriptionResponse, Boolean> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean isSuccess, boolean empty) {
                super.updateItem(isSuccess, empty);
                if (empty || isSuccess == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if (isSuccess) {
                        setText("SUCCESS");
                        setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    } else {
                        setText("PENDING");
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void switchView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(Main.getContext()::getBean);
            Parent view = loader.load();
            mainPane.setCenter(view);
        } catch (Exception e) {
            System.err.println("Failed to switch view to: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML public void handleMigrationScriptsClick(ActionEvent event) { switchView("/MigrationScripts.fxml"); }
    @FXML public void handelDatabaseConnection(ActionEvent event) { switchView("/DatabaseConnections.fxml"); }
    @FXML public void handelDatabaseTables(ActionEvent event) { switchView("/DatabaseTables.fxml"); }
    @FXML public void handelActivityLogs(ActionEvent event) { switchView("/ActivityLogs.fxml"); }
    @FXML public void handelRunHsitory(ActionEvent event) { switchView("/RunHistory.fxml"); }
}
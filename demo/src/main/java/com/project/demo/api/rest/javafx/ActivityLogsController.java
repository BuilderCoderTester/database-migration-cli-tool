package com.project.demo.api.rest.javafx;

import com.project.demo.modules.migration.dto.logs.response.MigrationLogsResponseDto;
import com.project.demo.modules.migration.service.LogService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ActivityLogsController {

    @FXML
    private TextField activitySearchField;

    @FXML
    private VBox timelineContainer;

    @FXML
    private VBox detailInspectorContainer;

    @Autowired
    private LogService logService;

    // Cache the master logs list for front-end real-time filtering
    private List<MigrationLogsResponseDto> masterLogsList;
    private ListView<MigrationLogsResponseDto> timelineListView;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        refreshActivityLogs();

        // Listen for real-time text input matching the search field
        activitySearchField.textProperty().addListener((obs, oldText, newText) -> filterTimeline(newText));
    }

    @FXML
    public void handleRefresh() {
        refreshActivityLogs();
    }

    private void refreshActivityLogs() {
        try {
            masterLogsList = logService.getAllActivities();

            if (masterLogsList == null || masterLogsList.isEmpty()) {
                showEmptyTimelinePlaceholder();
                showEmptyDetailsPlaceholder();
            } else {
                buildTimelineListView(masterLogsList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildTimelineListView(List<MigrationLogsResponseDto> logs) {
        timelineContainer.getChildren().clear();
        timelineContainer.setAlignment(Pos.TOP_LEFT);
        timelineContainer.setPadding(new Insets(15));

        // Create Header Row for Timeline Panel
        HBox headerRow = new HBox();
        Label title = new Label("Timeline");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #03363d;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label countLabel = new Label(logs.size() + " Events");
        countLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        headerRow.getChildren().addAll(title, spacer, countLabel);

        // Build the interactive ListView
        timelineListView = new ListView<>();
        VBox.setVgrow(timelineListView, Priority.ALWAYS);
        timelineListView.getStyleClass().add("clean-scroll");

        // Custom renderer factory to create dynamic visual cells matching the timeline list
        timelineListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(MigrationLogsResponseDto log, boolean empty) {
                super.updateItem(log, empty);
                if (empty || log == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox card = new HBox(10);
                    card.setAlignment(Pos.CENTER_LEFT);
                    card.getStyleClass().add("timeline-card");

                    Label icon = new Label("🕒");
                    icon.setStyle("-fx-text-fill: #14b8a6; -fx-font-size: 14px;");

                    VBox textWrapper = new VBox(4);
                    HBox.setHgrow(textWrapper, Priority.ALWAYS);
                    Label msg = new Label(log.message());
                    msg.setWrapText(true);
                    msg.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");

                    // Tiny bottom accent lines mimicking UI details
                    HBox pillContainer = new HBox(4);
                    Label pill1 = new Label(); pill1.getStyleClass().add("mini-pill");
                    Label pill2 = new Label(); pill2.getStyleClass().add("mini-pill");
                    pillContainer.getChildren().addAll(pill1, pill2);
                    textWrapper.getChildren().addAll(msg, pillContainer);

                    Label arrow = new Label("›");
                    arrow.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");

                    card.getChildren().addAll(icon, textWrapper, arrow);
                    setGraphic(card);
                }
            }
        });

        timelineListView.getItems().setAll(logs);

        // Add selection listener event trigger to display inspect details panel
        timelineListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                showActivityDetails(newSel);
            }
        });

        timelineContainer.getChildren().addAll(headerRow, timelineListView);
    }

    private void showActivityDetails(MigrationLogsResponseDto log) {
        detailInspectorContainer.getChildren().clear();
        detailInspectorContainer.setAlignment(Pos.TOP_LEFT);
        detailInspectorContainer.setPadding(new Insets(20));

        // --- Panel Header Layout ---
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label heading = new Label("Activity Details");
        heading.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #03363d;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label("🕒 " + (log.level() != null ? log.level().toString() : "RUNNING"));
        statusBadge.getStyleClass().add("status-running-badge");
        topRow.getChildren().addAll(heading, spacer, statusBadge);

        // --- Meta Grid Data Box Fields ---
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        grid.add(createMetaBox("⚙️ Operation", "Migration Applied"), 0, 0);
        grid.add(createMetaBox("📦 Migration Version", "V" + log.id()), 1, 0);
        grid.add(createMetaBox("📅 Executed At", log.timestamp() != null ? log.timestamp().format(DATE_FORMATTER) : "-"), 0, 1);
        grid.add(createMetaBox("⏱️ Duration", "-"), 1, 1);
        grid.add(createMetaBox("👤 Executed By", "System"), 0, 2);

        // --- Message Block Section ---
        VBox msgBox = new VBox(5);
        Label msgHeading = new Label("Message");
        msgHeading.setStyle("-fx-text-fill: #03363d; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label msgContent = new Label(log.message());
        msgContent.setStyle("-fx-text-fill: #334155; -fx-font-size: 13px;");
        msgBox.getChildren().addAll(msgHeading, msgContent);

        // --- Raw Logs JSON Block Terminal Section ---
        VBox rawBox = new VBox(5);
        VBox.setVgrow(rawBox, Priority.ALWAYS);
        Label rawHeading = new Label("Raw Log");
        rawHeading.setStyle("-fx-text-fill: #03363d; -fx-font-weight: bold; -fx-font-size: 12px;");

        TextArea rawLogArea = new TextArea();
        rawLogArea.setEditable(false);
        rawLogArea.getStyleClass().add("raw-log-area");
        VBox.setVgrow(rawLogArea, Priority.ALWAYS);

        // Generate formatting template mapping real object structures
        String jsonFormat = String.format(
                "{\n  \"id\": %d,\n  \"level\": \"%s\",\n  \"message\": \"%s\",\n  \"timestamp\": \"%s\"\n}",
                log.id(), log.level(), log.message(), log.timestamp()
        );
        rawLogArea.setText(jsonFormat);
        rawBox.getChildren().addAll(rawHeading, rawLogArea);

        // Combine into container workspace
        detailInspectorContainer.getChildren().addAll(topRow, grid, msgBox, rawBox);
    }

    private VBox createMetaBox(String fieldTitle, String fieldValue) {
        VBox box = new VBox(4);
        box.getStyleClass().add("meta-box");
        Label title = new Label(fieldTitle);
        title.getStyleClass().add("meta-label");
        Label value = new Label(fieldValue);
        value.getStyleClass().add("meta-value");
        if ("System".equals(fieldValue)) {
            value.setStyle("-fx-font-weight: bold;");
        }
        box.getChildren().addAll(title, value);
        return box;
    }

    private void filterTimeline(String query) {
        if (masterLogsList == null) return;

        if (query == null || query.isBlank()) {
            timelineListView.getItems().setAll(masterLogsList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            List<MigrationLogsResponseDto> filtered = masterLogsList.stream()
                    .filter(log -> log.message() != null && log.message().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList());
            timelineListView.getItems().setAll(filtered);
        }
    }

    private void showEmptyTimelinePlaceholder() {
        timelineContainer.getChildren().clear();
        timelineContainer.setAlignment(Pos.CENTER);
        Label icon = new Label("🕒"); icon.getStyleClass().add("placeholder-icon-large");
        Label title = new Label("No Activity Found"); title.getStyleClass().add("placeholder-title-bold");
        Label subtitle = new Label("No migration activity has been recorded yet."); subtitle.getStyleClass().add("page-subtitle");
        timelineContainer.getChildren().addAll(icon, title, subtitle);
    }

    private void showEmptyDetailsPlaceholder() {
        detailInspectorContainer.getChildren().clear();
        detailInspectorContainer.setAlignment(Pos.CENTER);
        Label icon = new Label("📈"); icon.getStyleClass().add("placeholder-icon-pulse-large");
        Label title = new Label("No Activity Selected"); title.getStyleClass().add("placeholder-title-bold");
        Label subtitle = new Label("Select an activity from the timeline to view its complete details."); subtitle.getStyleClass().add("page-subtitle");
        detailInspectorContainer.getChildren().addAll(icon, title, subtitle);
    }
}
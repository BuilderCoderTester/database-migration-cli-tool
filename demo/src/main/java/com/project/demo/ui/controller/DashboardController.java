package com.project.demo.ui.controller;

import com.project.demo.core.MigrationEngine;
import com.project.demo.utility.Helper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class DashboardController {

    @Autowired
    private MigrationEngine engine;

    @Autowired
    private  Helper helper;

    @FXML
    private Label currentVersion;

    @FXML
    private Label statusLabel;

    @FXML
    private Label pendingCount;

    @FXML
    private TextArea logArea;

    @FXML
    public void initialize() {
        refresh();
    }

    @FXML
    public void refresh() {
        currentVersion.setText(helper.getCurrentVersion().orElse("None"));
        pendingCount.setText(String.valueOf(helper.getPendingCount()));

        boolean dirty = helper.isDatabaseDirty();
        statusLabel.setText(dirty ? "DIRTY" : "CLEAN");

        // optional styling
        statusLabel.setStyle(dirty
                ? "-fx-text-fill: #F44747;"
                : "-fx-text-fill: #6A9955;");
    }

    @FXML
//    public void runMigration() {
//
//        log("Starting migration...");
//
//        // 🔥 Run in background thread
//        Task<Void> task = new Task<>() {
//            @Override
//            protected Void call() {
//
//                try {
//                    helper.migrateAll();
//
//                    Platform.runLater(() -> {
//                        log("Migration completed successfully");
//                        refresh();
//                    });
//
//                } catch (Exception e) {
//
//                    Platform.runLater(() ->
//                            log("ERROR: " + e.getMessage())
//                    );
//                }
//
//                return null;
//            }
//        };
//
//        new Thread(task).start();
//    }

    private void log(String msg) {
        logArea.appendText(msg + "\n");
    }
}
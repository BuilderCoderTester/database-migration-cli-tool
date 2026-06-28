package com.project.demo.javafx;

import com.project.demo.model.Migration;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

public class DashboardController {
    @FXML
    private TableView<Migration> migrationTable;

    @FXML
    private TableColumn<Migration,String> versionColumn;

    @FXML
    private TableColumn<Migration,String> descriptionColumn;

    @FXML
    private TableColumn<Migration,String> appliedColumn;

    @FXML
    private TableColumn<Migration,String> durationColumn;

    @FXML
    private TableColumn<Migration,String> statusColumn;

    @FXML
    private ListView<String> activityList;

    @FXML
    private BorderPane mainPane;

    @FXML
    public void initialize() {

        versionColumn.setCellValueFactory(
                new PropertyValueFactory<>("version"));

        descriptionColumn.setCellValueFactory(
                new PropertyValueFactory<>("description"));

        appliedColumn.setCellValueFactory(
                new PropertyValueFactory<>("appliedOn"));

        durationColumn.setCellValueFactory(
                new PropertyValueFactory<>("duration"));

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status"));

        migrationTable.getItems().addAll(
                new Migration(
                        "VV004",
                        "insert into sing",
                        "2026-06-11",
                        "76 ms",
                        "Applied"
                ),
                new Migration(
                        "VV006",
                        "create table demo",
                        "2026-06-11",
                        "72 ms",
                        "Applied"
                )
        );

        activityList.getItems().addAll(
                "VV005_create_table_sing.sql Applied",
                "VV004_insert_into_sing.sql Applied",
                "VV003_create_table_tree.sql Applied"
        );
    }

//    @FXML
//    private void openConnections() {
//
//        try {
//
//            FXMLLoader loader =
//                    new FXMLLoader(getClass()
//                            .getResource("/ConnectionView.fxml"));
//            loader.setControllerFactory(
//                    SpringContext.getApplicationContext()::getBean);
//            Parent view = loader.load();
//
//            mainPane.setCenter(view);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}

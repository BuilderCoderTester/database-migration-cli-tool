package com.project.demo.javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
        Scene scene =
                new Scene(fxmlLoader.load(), 1800, 900);

        scene.getStylesheets().add(
                getClass()
                        .getResource("/style.css")
                        .toExternalForm());

        stage.setTitle("MigrateDB");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}

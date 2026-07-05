package com.project.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class Main extends Application {

    private static ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(DemoApplication.class)
                .web(WebApplicationType.NONE).run();
    }

    @Override
    public void start(Stage stage) throws Exception {

        System.out.println("1. Start method called");

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/Dashboard.fxml"));

        loader.setControllerFactory(context::getBean);

        System.out.println("2. Loading FXML");

        Parent root = loader.load();

        System.out.println("3. FXML Loaded");

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("MigrateDB");
        stage.centerOnScreen();

        System.out.println("4. Showing Stage");

        stage.show();

        System.out.println("5. Stage Shown");
    }

    @Override
    public void stop() {
        context.close();
    }

    public static ConfigurableApplicationContext getContext() {
        return context;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
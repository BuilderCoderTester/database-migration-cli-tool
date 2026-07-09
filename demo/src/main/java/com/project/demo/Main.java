package com.project.demo;

import com.project.demo.config.ConfigManager;
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
//        context = new SpringApplicationBuilder(DemoApplication.class)
//                .web(WebApplicationType.NONE).run();
    }
    public static void startSpring() {

        if (context == null) {

            context = new SpringApplicationBuilder(DemoApplication.class)
                    .web(WebApplicationType.NONE)
                    .run();

        }

    }

    @Override
    public void start(Stage stage) throws Exception {

        System.out.println("1. Start method called");

        ConfigManager manager = new ConfigManager();

        FXMLLoader loader;

        if (manager.configExists()) {

            startSpring();

            loader = new FXMLLoader(
                    getClass().getResource("/Dashboard.fxml"));

            loader.setControllerFactory(context::getBean);

        } else {

            loader = new FXMLLoader(
                    getClass().getResource("/Setup.fxml"));

        }

        Parent root = loader.load();

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("Migration Tool");
        stage.centerOnScreen();
        stage.show();
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
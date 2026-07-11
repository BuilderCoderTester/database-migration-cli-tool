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
        ConsoleLogger.info("JavaFX lifecycle: init() hook invoked.");
    }

    public static void startSpring() {
        if (context == null) {
            ConsoleLogger.info("Starting Spring Boot Context in headless mode (No Web Server)...");
            try {
                context = new SpringApplicationBuilder(DemoApplication.class)
                        .web(WebApplicationType.NONE)
                        .run();
                ConsoleLogger.success("Spring Boot Application Context loaded successfully!");
            } catch (Exception e) {
                ConsoleLogger.error("Failed to initialize Spring Boot context: " + e.getMessage());
                throw e;
            }
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        ConsoleLogger.info("JavaFX lifecycle: start() method called. Preparing Window UI.");

        ConfigManager manager = new ConfigManager();
        FXMLLoader loader;

        // 1. Detect view type
        ConsoleLogger.info("Checking for existing database configuration file...");
        boolean isDashboard = manager.configExists();

        if (isDashboard) {
            ConsoleLogger.success("Configuration detected. Routing application to Dashboard View.");
            startSpring();
            loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            loader.setControllerFactory(context::getBean);
        } else {
            ConsoleLogger.warning("No configuration found. Routing application to Setup Wizard View.");
            loader = new FXMLLoader(getClass().getResource("/Setup.fxml"));
        }

        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Migration Tool");

        // 2. Apply Responsive Window Guardrails
        if (isDashboard) {
            stage.setMinWidth(1100);
            stage.setMinHeight(750);
            stage.setMaximized(true);
            ConsoleLogger.info("Applied responsive constraints to Dashboard view (Defaulting to Maximized).");
        } else {
            stage.setMinWidth(600);
            stage.setMinHeight(500);
            stage.setResizable(false);
            ConsoleLogger.info("Applied fixed boundaries to Setup Wizard view.");
        }

        stage.centerOnScreen();
        stage.show();
        ConsoleLogger.success("Application window successfully rendered on screen.");
    }

    @Override
    public void stop() {
        ConsoleLogger.warning("Application close request detected. Executing graceful shutdown procedures...");
        if (context != null) {
            context.close();
            ConsoleLogger.success("Spring Context closed gracefully.");
        }
        ConsoleLogger.info("JavaFX application lifecycle stopped cleanly.");
    }

    public static ConfigurableApplicationContext getContext() {
        return context;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
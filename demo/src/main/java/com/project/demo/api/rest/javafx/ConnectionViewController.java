package com.project.demo.api.rest.javafx;

import com.project.demo.Main;
import com.project.demo.modules.migration.model.ConnectionConfig;
import com.project.demo.modules.migration.service.ConnectionService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Component
public class ConnectionViewController {

    @FXML
    private FlowPane cardContainer;

    private final ConnectionService connectionService;

    public ConnectionViewController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @FXML
    public void initialize() throws SQLException {

        List<ConnectionConfig> connections =
                connectionService.getAllConnections();

        connections.forEach(this::addCard);
    }

    private void addCard(ConnectionConfig conn) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ConnectionCard.fxml"));

            loader.setControllerFactory(
                    Main.getContext()::getBean);

            VBox card = loader.load();

            ConnectionCardController controller = loader.getController();

            controller.setData(conn);

            cardContainer.getChildren().add(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void addConnection() {
        System.out.println("Open Add Connection Dialog");
    }
}
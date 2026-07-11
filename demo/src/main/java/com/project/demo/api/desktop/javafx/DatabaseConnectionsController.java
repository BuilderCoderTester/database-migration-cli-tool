package com.project.demo.api.desktop.javafx;

import com.project.demo.Main;
import com.project.demo.modules.migration.model.ConnectionConfig;
import com.project.demo.modules.migration.service.ConnectionService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Component
public class DatabaseConnectionsController {
    @FXML
    private BorderPane dashboardConnection;
    @FXML
    private TilePane connectionTilePane;

    @Autowired
    private ConnectionService connectionService;

    @FXML
    public void initialize() throws IOException, SQLException {

        List<ConnectionConfig> connections =
                connectionService.getAllConnections();

        loadConnections(connections);
    }

    private void loadConnections(List<ConnectionConfig> connections) throws IOException {

        connectionTilePane.getChildren().clear();

        for (ConnectionConfig connection : connections) {

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/ConnectionCard.fxml"));

            loader.setControllerFactory(Main.getContext()::getBean);

            Parent card = loader.load();

            ConnectionCardController controller = loader.getController();

            controller.setData(connection);

            connectionTilePane.getChildren().add(card);
        }
    }

    public void addNewConnections(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddConnectionDialog.fxml"));
        loader.setControllerFactory(Main.getContext()::getBean);
        Parent parent = loader.load();

        Stage stage = new Stage();
        stage.setTitle("Add Connection");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(parent));
        stage.showAndWait();
    }
}

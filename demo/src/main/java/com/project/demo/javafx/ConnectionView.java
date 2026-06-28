package com.project.demo.javafx;

import com.project.demo.model.ConnectionConfig;
import com.project.demo.service.ConnectionService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConnectionView {
    @FXML
    private FlowPane cardContainer;

    @Autowired
    private ConnectionService connectionService;

//    @FXML
//    public void initialize() {
//
//      List<ConnectionConfig> connections =
//              connectionService.getAllConnections();
//
//        connections.forEach(this::addCard);
//    }
//
//    private void addCard(ConnectionConfig conn) {
//
//        try {
//
//            FXMLLoader loader =
//                    new FXMLLoader(getClass()
//                            .getResource("/ConnectionCard.fxml"));
//
//            VBox card = loader.load();
//
//            ConnectionCard controller =
//                    loader.getController();
//
//            controller.setData(conn);
//
//            cardContainer.getChildren().add(card);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    @FXML
    private void addConnection() {
        System.out.println("Open Add Connection Dialog");
    }
}

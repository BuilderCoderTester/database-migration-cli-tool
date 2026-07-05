package com.project.demo.api.rest.javafx;

import com.project.demo.modules.migration.dto.connection.request.ConnectionRequest;
import com.project.demo.modules.migration.service.ConnectionService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AddConnectionController {
    @FXML
    private TextField connectionNameField;

    @FXML
    private TextField hostField;

    @FXML
    private TextField portField;

    @FXML
    private TextField databaseField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField schemaField;

    @FXML
    private VBox root;

    @Autowired
    private ConnectionService connectionService;

    @FXML
    public void handleCancel(ActionEvent actionEvent) {
        ((Stage) root.getScene().getWindow()).close();
    }

    @FXML
    public void handleTestConnection(ActionEvent actionEvent) {
        String connectionName = connectionNameField.getText();
        String host = hostField.getText();
        int port = Integer.parseInt(portField.getText());
        String database = databaseField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String schema = schemaField.getText();

        System.out.println("Connection Name: " + connectionName);
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Database: " + database);
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
        System.out.println("Schema: " + schema);

        ConnectionRequest connectionRequest = new ConnectionRequest();
        connectionRequest.setName(connectionName);
        connectionRequest.setHost(host);
        connectionRequest.setPort(port);
        connectionRequest.setDatabase(database);
        connectionRequest.setUsername(username);
        connectionRequest.setPassword(password);
        connectionRequest.setSchema(schema);

        connectionService.connect(connectionRequest);

    }

    @FXML
    public void handleSaveConnection(ActionEvent actionEvent) {
        String connectionName = connectionNameField.getText();
        String host = hostField.getText();
        int port = Integer.parseInt(portField.getText());
        String database = databaseField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String schema = schemaField.getText();

        System.out.println("Connection Name: " + connectionName);
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Database: " + database);
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
        System.out.println("Schema: " + schema);

        ((Stage) root.getScene().getWindow()).close();
    }
}

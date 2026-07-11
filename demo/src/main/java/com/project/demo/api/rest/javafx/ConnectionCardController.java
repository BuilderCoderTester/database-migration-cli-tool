package com.project.demo.api.rest.javafx;

import com.project.demo.modules.migration.model.ConnectionConfig;
import com.project.demo.modules.migration.service.ConnectionService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ConnectionCardController {

    @Autowired
    private ConnectionService connectionService;

    @FXML
    private Button activeBtn;
    @FXML
    private Label dbLabel;
    public void setData(ConnectionConfig connection) {

        dbLabel.setText(connection.getDatabase());
        activeBtn.setUserData(connection.getDatabase());

    }

    public void setActive(ActionEvent actionEvent) throws SQLException, IOException {
        String databaseName = (String) activeBtn.getUserData();

        Connection conn = connectionService.activeConnection(databaseName);
        PreparedStatement dbStmt = conn.prepareStatement("SELECT current_database()");
        ResultSet dbRs = dbStmt.executeQuery();
        if (dbRs.next()) {
            System.out.println("Connected to database {}"+ dbRs.getString(1));
        }

        connectionService.createSystemTables(conn);
    }

    public void handleDelete(ActionEvent actionEvent){

    }
}

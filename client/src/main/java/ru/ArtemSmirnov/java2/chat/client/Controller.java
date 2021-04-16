package ru.ArtemSmirnov.java2.chat.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private static final Logger logger = LogManager.getLogger(Controller.class.getName());

    @FXML
    TextField msgField, loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    TextArea msgArea;

    @FXML
    VBox loginPanel, msgPanel;

    @FXML
    ListView<String> clientsList;

    private Network network;
    private String username;
    private HistoryFileLogger fileLogger;

    public void setUsername(String username) {
        this.username = username;
        boolean usernameIsNull = username == null;
        loginPanel.setVisible(usernameIsNull);
        loginPanel.setManaged(usernameIsNull);
        msgPanel.setVisible(!usernameIsNull);
        msgPanel.setManaged(!usernameIsNull);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUsername(null);
        network = new Network();

        network.setOnCommandReceivedCallback(args -> {
            String cmd = (String) args[0];
            executeCommand(cmd);
        });

        network.setOnMessageReceivedCallback(args -> {
            String msg = (String) args[0];
            fileLogger.writeFile(msg + "\n");
            msgArea.appendText(msg + "\n");
        });

        network.setOnDisconnectCallback(args -> {
            loginField.clear();
            passwordField.clear();
            msgArea.clear();
            Platform.runLater(() -> clientsList.getItems().clear());
            setUsername(null);
            fileLogger.close();
        });
    }

    public void login() {
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            showErrorAlert("Логин/пароль не могут быть пустыми");
            return;
        }
        if (!network.isConnected()) {
            try {
                network.connect(8189);
            } catch (IOException e){
                logger.throwing(Level.ERROR, e);
                showErrorAlert("Невозможно подключится к серверу на порт " + 8189);
                return;
            }
        }
        try {
            network.tryToLogin(loginField.getText(), passwordField.getText());
        } catch (IOException e) {
            logger.throwing(Level.ERROR, e);
            showErrorAlert("Невозможно отправить данные пользователя");
        }
    }

    public void executeCommand(String msg) {
        String cmd = msg.split("\\s")[0];
        switch (cmd) {
            case "/login_ok":
                setUsername(msg.split("\\s", 2)[1]);
                fileLogger.init(msg.split("\\s", 2)[1]);
                msgArea.clear();
                msgArea.appendText(fileLogger.readFile());
                return;
            case "/login_failed":
            case "/w_failed":
            case "/change_nick_false":
                Platform.runLater(() -> showErrorAlert(msg.split("\\s", 2)[1]));
                return;
            case "/clients_list":
                String[] tokens = msg.split("\\s");
                Platform.runLater(() -> {
                    clientsList.getItems().clear();
                    for (int i = 1; i < tokens.length; i++) {
                        clientsList.getItems().add(tokens[i]);
                    }
                });
                return;
            case "/exit":
                return;
            case "/change_nick_ok":
                String newUsername = msg.split("\\s+", 2)[1];
                username = newUsername;
                fileLogger.renameFile(newUsername);
                return;
        }
    }

    public void sendMessage() {
        try {
            network.sendMessage(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            logger.throwing(Level.ERROR, e);
            showErrorAlert("Невозможно отправить сообщение");
        }

    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Simple Chat");
        alert.setContentText(message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public void logout() {
        network.disconnect();
    }
}

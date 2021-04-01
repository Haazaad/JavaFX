package ru.ArtemSmirnov.java2.chat.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
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

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private HistoryFileLogger logger;

    public void setUsername(String username) {
        this.username = username;
        logger = new HistoryFileLogger(username);
        boolean usernameIsNull = username == null;
        loginPanel.setVisible(usernameIsNull);
        loginPanel.setManaged(usernameIsNull);
        msgPanel.setVisible(!usernameIsNull);
        msgPanel.setManaged(!usernameIsNull);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUsername(null);
    }

    public void login() {
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            showErrorAlert("Логин/пароль не могут быть пустыми");
            return;
        }
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/login " + loginField.getText() + " " + passwordField.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        disconnect();
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/")) {
                            executeCommand(msg);
                            continue;
                        }
                        logger.writeFile(msg + "\n");
                        msgArea.appendText(msg + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }

            });
            t.start();
        } catch (IOException e) {
            showErrorAlert("Невозможно подключиться к серверу");
        }
    }

    public void executeCommand(String msg) {
        String cmd = msg.split("\\s")[0];
        switch (cmd) {
            case "/login_ok":
                setUsername(msg.split("\\s", 2)[1]);
                msgArea.appendText(logger.readFile());
                return;
            case "/login_failed":
            case "/w_failed":
            case "/change_nick_false":
                Platform.runLater(() -> showErrorAlert(msg.split("\\s", 2)[1]));
                return;
            case "/clients_list":
                String[] tokens = msg.split("\\s");
                Platform.runLater(() -> {
                    System.out.println(Thread.currentThread().getName());
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
                logger.renameFile(newUsername);
                return;
        }
    }

    public void sendMessage() {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            showErrorAlert("Невозможно отправить сообщение");
        }

    }

    private void disconnect() {
        loginField.clear();
        passwordField.clear();
        msgArea.clear();
        Platform.runLater(() -> clientsList.getItems().clear());
        setUsername(null);
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Simple Chat");
        alert.setContentText(message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

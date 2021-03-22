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
    TextField msgField, usernameField;

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

    public void setUsername(String username) {
        this.username = username;
        if (username != null) {
            loginPanel.setVisible(false);
            loginPanel.setManaged(false);
            msgPanel.setVisible(true);
            msgPanel.setManaged(true);
        } else {
            loginPanel.setVisible(true);
            loginPanel.setManaged(true);
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUsername(null);
    }

    public void login() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        if (usernameField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Логин/пароль не могут быть пустыми", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        try {
            out.writeUTF("/login " + usernameField.getText() + " " + passwordField.getText());
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
            Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно подключиться к серверу", ButtonType.OK);
            alert.showAndWait();
        }
    }

    /**
     * Метод обработки всех служебных комманд, начинающихся с "/"
     * @param msg - введенное сообщение
     */
    public void executeCommand(String msg) {
        String cmd = msg.split("\\s")[0];
        switch (cmd) {
            case "/login_ok":
                setUsername(msg.split("\\s", 2)[1]);
                return;
            case "/login_failed":
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, msg.split("\\s", 2)[1], ButtonType.OK);
                    alert.showAndWait();
                });
                return;
            case "/w_failed":
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, msg.split("\\s", 2)[1], ButtonType.OK);
                    alert.showAndWait();
                });
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
        }
    }

    public void sendMessage() {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно отправить сообщение");
            alert.showAndWait();
        }

    }

    // целесообразно ли делать метод public?
    private void disconnect() {
        usernameField.clear();
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
}

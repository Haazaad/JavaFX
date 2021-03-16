package ru.ArtemSmirnov.java2.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                // цикл авторизации
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith("/login ")) {
                        String usernameFromLogin = msg.split("\\s")[1];
                        if (server.isUserOnline(usernameFromLogin)) {
                            sendMessage("/login_failed Текущий никнейм занят");
                            continue;
                        }
                        username = usernameFromLogin;
                        sendMessage("/login_ok " + username);
                        server.subscribe(this);
                        break;
                    }
                }
                // цикл общения
                while (true) {
                    String msg = in.readUTF();
                    System.out.println(msg);
                    if (msg.startsWith("/")) {
                        String[] tokens = msg.split("\\s");
                        executeCommand(tokens);
                        continue;
                    }
                    server.broadcastMessage(username + ": " + msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();

    }

    private void executeCommand(String[] tokens){
        switch (tokens[0]) {
            case "/w":
                server.sendPrivateMessage(this, tokens[1], tokens[2]);
                return;
            case "/exit":
                disconnect();
                return;
            case "/stat":
                sendMessage("Общее количество сообщений - " + server.getCountAllMessage());
                return;
            case "/who_am_i":
                sendMessage("Текущий ник " + username);
                return;
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

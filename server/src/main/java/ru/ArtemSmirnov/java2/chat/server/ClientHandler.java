package ru.ArtemSmirnov.java2.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

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
                        if (server.isNickBusy(usernameFromLogin)) {
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
                        switch (msg) {
                            case "/exit":
                                disconnect();
                                continue;
                            case "/stat":
                                sendMessage("Общее количество сообщений - " + server.getCountAllMessage());
                                continue;
                            case "/who_am_i":
                                sendMessage(username);
                                continue;
                        }
                    }
                    if (msg.startsWith("/w ")) {
                        String sendTo = msg.split("\\s", 3)[1];
                        if (!server.isNickBusy(sendTo)) {
                            String message = "/w_fail Невозможно отправить сообщение пользователю " + sendTo + " пользователь не в сети";
                            sendMessage(message);
                            continue;
                        }
                        String message = getCurrentTime() + " " + username + ": " + msg.split("\\s", 3)[2];
                        server.sendPrivateMessage(username, message);
                        server.sendPrivateMessage(sendTo, message);
                        continue;
                    }
                    server.broadcastMessage(getCurrentTime() + " " + username + ": " + msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
               disconnect();
            }
        }).start();

    }

    public void sendMessage(String message) throws IOException{
        out.writeUTF(message);
    }

    public String getCurrentTime() {
        Date date = new Date();
        SimpleDateFormat formatDate = new SimpleDateFormat("hh:mm:ss");
        return formatDate.format(date);
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

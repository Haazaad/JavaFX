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
    private long messageCount;

    public String getUsername() {
        return username;
    }

    public void riseMessageCount() {
        messageCount++;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        messageCount = 0;

        new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readUTF();
                    System.out.println(msg);
                    if (msg.startsWith("/")) {
                        executeCommand(msg);
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

    /**
     * Метод обработки всех служебных комманд, начинающихся с "/"
     * @param message - введенное сообщение
     */
    private void executeCommand(String message){
        String[] tokens = message.split("\\s");
        switch (tokens[0]) {
            case "/login":
                tokens = message.split("\\s", 3);
                server.validateUser(this, tokens[1], tokens[2]);
                return;
            case "/w":
                tokens = message.split("\\s", 3);
                server.sendPrivateMessage(this, tokens[1], tokens[2]);
                return;
            case "/exit":
                disconnect();
                return;
            case "/stat":
                sendMessage("Количество сообщений в чате - " + getMessageCount());
                System.out.println("Общее количество сообщений " + server.getCountAllMessage());
                return;
            case "/who_am_i":
                sendMessage("Текущий ник " + username);
                return;
            case "/change_nick":
                tokens = message.split("\\s", 2);
                server.broadcastMessage("Пользователь " + username + " сменил никнейм на " + tokens[1]);
                setUsername(tokens[1]);
                server.broadcastClientList();
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

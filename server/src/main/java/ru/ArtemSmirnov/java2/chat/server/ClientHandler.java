package ru.ArtemSmirnov.java2.chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable{
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private int userId;
    private String username;
    private long messageCount;

    private static final Logger logger = LogManager.getLogger(ClientHandler.class.getName());

    public String getUsername() {
        return username;
    }

    public int getUserId() {
        return userId;
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

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String msg = in.readUTF();
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
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        messageCount = 0;
    }

    private void executeCommand(String message){
        String[] tokens = message.split("\\s+");
        switch (tokens[0]) {
            case "/login":
                if (tokens.length != 3) {
                    sendMessage("/login_failed Введите имя пользователя и пароль");
                    return;
                }
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
                logger.debug("/who_am_i Текущий ник " + username);
                return;
            case "/change_nick":
                if (tokens.length != 2) {
                    sendMessage("/change_nick_false Введена некорректная команда");
                    return;
                }
                if (server.getAuthenticationProvider().isUserOnline(tokens[1])) {
                    logger.debug("Ошибка смена никнейма для пользователя " + tokens[1]);
                    sendMessage("/change_nick_false Введенный никнейм занят");
                    return;
                }
                server.getAuthenticationProvider().changeNickname(userId, tokens[1]);
                server.broadcastMessage("Пользователь " + username + " сменил никнейм на " + tokens[1]);
                logger.debug("Пользователь " + username + " сменил никнейм на " + tokens[1]);
                sendMessage("/change_nick_ok " + tokens[1]);
                setUsername(tokens[1]);
                server.broadcastClientList();
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

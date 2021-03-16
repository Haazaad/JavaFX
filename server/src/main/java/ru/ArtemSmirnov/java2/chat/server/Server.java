package ru.ArtemSmirnov.java2.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private Users users;

    private long countAllMessage;

    public synchronized long getCountAllMessage() {
        return countAllMessage;
    }

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
        users = new Users();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);
            while (true) {
                System.out.println("Ждем нового клиента...");
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        System.out.println("Клиент подключился");
        broadcastMessage("Пользователь " + clientHandler.getUsername() + " присоединился к чату.");
        broadcastClientList();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        broadcastMessage("Пользователь " + clientHandler.getUsername() + " покинул чат.");
        clients.remove(clientHandler);
        System.out.println("Клиент отключился");
        broadcastClientList();
    }

    public synchronized void broadcastMessage(String message){
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(getCurrentTime() + " " + message);
        }
        countAllMessage++;
    }

    public synchronized void sendPrivateMessage(ClientHandler fromUser, String sendToUser, String message){
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(sendToUser)) {
                client.sendMessage(getCurrentTime() + " От: " + fromUser.getUsername() + " Сообщение: " + message);
                fromUser.sendMessage(getCurrentTime() + " Пользователю: " + sendToUser + " Сообщение: " + message);
                client.riseMessageCount();
                fromUser.riseMessageCount();
                return;
            }
        }
        fromUser.sendMessage("/w_failed Невозможно отправить сообщение пользователю " + sendToUser + " - пользователь не в сети");
    }

    public synchronized boolean isUserOnline(String username) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Метод валидации введенной пары логин/пароль
     * @param clientHandler - поток обработчик
     * @param login - логин пользователя
     * @param password - введенный пароль пользователя
     */
    public void validateUser(ClientHandler clientHandler, String login, String password) {
        if (users.havingUser(login)) {
            String username = users.returnUsername(login, password);
            if (username != null) {
                if (!isUserOnline(username)) {
                    clientHandler.setUsername(username);
                    subscribe(clientHandler);
                    clientHandler.sendMessage("/login_ok " + username);
                    return;
                }
                clientHandler.sendMessage("/login_failed Текущий никнейм занят");
                return;
            }
            clientHandler.sendMessage("/login_failed Введен некорректный пароль");
            return;
        }
        clientHandler.sendMessage("/login_failed Ошибка авторизации - введена не корректная пара логин/пароль");
    }

    public String getCurrentTime() {
        Date date = new Date();
        SimpleDateFormat formatDate = new SimpleDateFormat("hh:mm:ss");
        return formatDate.format(date);
    }

    public synchronized void broadcastClientList() {
        StringBuilder builder = new StringBuilder("/clients_list ");
        for (ClientHandler c: clients) {
            builder.append(c.getUsername()).append(" ");
        }
        builder.setLength(builder.length() - 1);
        String clientsList = builder.toString();
        for (ClientHandler c: clients) {
            c.sendMessage(clientsList);
        }
    }
}

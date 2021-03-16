package ru.ArtemSmirnov.java2.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> clients;

    private long countAllMessage;

    public long getCountAllMessage() {
        return countAllMessage;
    }

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);
            while (true) {
                System.out.println("Ждем нового клиента...");
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastClientList();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
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

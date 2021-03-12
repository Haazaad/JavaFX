package ru.ArtemSmirnov.java2.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
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

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public void broadcastMessage(String message) throws IOException{
        for (ClientHandler clientHandler: clients) {
            clientHandler.sendMessage(message);
        }
        countAllMessage++;
    }

    public boolean isNickBusy(String username) {
        for (ClientHandler clientHandler: clients) {
            if (clientHandler.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }
}

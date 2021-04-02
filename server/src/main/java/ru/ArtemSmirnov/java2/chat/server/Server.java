package ru.ArtemSmirnov.java2.chat.server;

import ru.ArtemSmirnov.java2.chat.server.authentication.AuthenticationProvider;
import ru.ArtemSmirnov.java2.chat.server.authentication.DbAuthenticationProvider;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private AuthenticationProvider authenticationProvider;
    private DbConnection dbConnection;
    private DbQueryProvider queryProvider;

    private long countAllMessage;

    public synchronized long getCountAllMessage() {
        return countAllMessage;
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    // Для прикручивания ExecutorService создаю фиксированный пул потоков
    // ClientHandler заимплементил от Runnable в нем переопределил run() на ожидание сообщения
    // Однако несовсем понял, при таком подходе по идее также поток должен висеть и опрашивать состояние входящего стрима
    // т.е. по идее надо как-то отправить его в ассинхрон на ожидание события и вот тут я зашел в тупик
    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
        this.dbConnection = new DbConnection();
        authenticationProvider = new DbAuthenticationProvider(dbConnection.getConnection());
        queryProvider = new DbQueryProvider(dbConnection.getConnection());
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);
            queryProvider.registerServer();
            while (true) {
                System.out.println("Ждем нового клиента...");
                Socket socket = serverSocket.accept();
                executorService.execute(new ClientHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
            queryProvider.unregisterServer();
            dbConnection.disconnect();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        System.out.println("Клиент подключился");
        queryProvider.registerClient(clientHandler);
        broadcastMessage("Пользователь " + clientHandler.getUsername() + " присоединился к чату.");
        broadcastClientList();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        broadcastMessage("Пользователь " + clientHandler.getUsername() + " покинул чат.");
        queryProvider.unregisterClient(clientHandler);
        clients.remove(clientHandler);
        System.out.println("Клиент отключился");
        broadcastClientList();
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(getCurrentTime() + " " + message);
        }
        countAllMessage++;
    }

    public synchronized void sendPrivateMessage(ClientHandler fromUser, String sendToUser, String message) {
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
        return authenticationProvider.isUserOnline(username);
    }

    public synchronized void validateUser(ClientHandler clientHandler, String login, String password) {
        List<String> cred = authenticationProvider.getCredentialsByLoginAndPassword(login, password);
        if (!cred.isEmpty()) {
            int userId = Integer.parseInt(cred.get(0));
            String userNickname = cred.get(1);
            if (!isUserOnline(userNickname)) {
                clientHandler.setUserId(userId);
                clientHandler.setUsername(userNickname);
                subscribe(clientHandler);
                clientHandler.sendMessage("/login_ok " + userNickname);
                return;
            }
            clientHandler.sendMessage("/login_failed Ошибка авторизации - учетная запись уже используется");
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
        for (ClientHandler c : clients) {
            builder.append(c.getUsername()).append(" ");
        }
        builder.setLength(builder.length() - 1);
        String clientsList = builder.toString();
        for (ClientHandler c : clients) {
            c.sendMessage(clientsList);
        }
    }
}

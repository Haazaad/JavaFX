package ru.ArtemSmirnov.java2.chat.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger logger = LogManager.getLogger(Server.class.getName());

    public synchronized long getCountAllMessage() {
        return countAllMessage;
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
        this.dbConnection = new DbConnection();
        authenticationProvider = new DbAuthenticationProvider(dbConnection.getConnection());
        queryProvider = new DbQueryProvider(dbConnection.getConnection());
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Сервер запущен на порту " + port);
            queryProvider.registerServer();
            while (true) {
                logger.info("Ждем нового клиента...");
                Socket socket = serverSocket.accept();
                executorService.execute(new ClientHandler(this, socket));
            }
        } catch (IOException e) {
            logger.throwing(Level.ERROR, e);
        } finally {
            executorService.shutdown();
            queryProvider.unregisterServer();
            dbConnection.disconnect();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        logger.info(clientHandler.getUsername() + " подключился к чату.");
        queryProvider.registerClient(clientHandler);
        broadcastMessage("Пользователь " + clientHandler.getUsername() + " присоединился к чату.");
        broadcastClientList();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        broadcastMessage("Пользователь " + clientHandler.getUsername() + " покинул чат.");
        queryProvider.unregisterClient(clientHandler);
        clients.remove(clientHandler);
        logger.info(clientHandler.getUsername() + " отключился от чата.");
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
                logger.trace("От: " + fromUser.getUsername() + " Сообщение: " + message);
                fromUser.sendMessage(getCurrentTime() + " Пользователю: " + sendToUser + " Сообщение: " + message);
                client.riseMessageCount();
                fromUser.riseMessageCount();
                return;
            }
        }
        logger.debug("/w_failed Невозможно отправить сообщение от " + fromUser.getUsername() + " пользователю " + sendToUser + " - пользователь не в сети");
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
                logger.trace("/login_ok " + userNickname);
                return;
            }
            logger.trace("/login_failed Ошибка авторизации - учетная запись " + clientHandler.getUsername() + " уже используется");
            clientHandler.sendMessage("/login_failed Ошибка авторизации - учетная запись уже используется");
            return;
        }
        logger.trace("/login_failed Ошибка авторизации - введена не корректная пара логин/пароль для " + clientHandler.getUsername());
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

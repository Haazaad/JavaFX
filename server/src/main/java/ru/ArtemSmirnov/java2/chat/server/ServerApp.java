package ru.ArtemSmirnov.java2.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {
    private static long count;

    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Сервер запущен на порту 8189. Ожидаем подключения клиента");
            Socket socket = serverSocket.accept();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Клиент подключился");

            while (true) {
                String msg = in.readUTF();
                switch (msg) {
                    case "/exit":
                        serverSocket.close();
                        break;
                    case "/stat":
                        out.writeUTF("Количество сообщений - " + count);
                        continue;
                }
                System.out.println(msg);
                count++;
                out.writeUTF(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

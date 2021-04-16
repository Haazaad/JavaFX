package ru.ArtemSmirnov.java2.chat.client;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Network {
    private static final Logger logger = LogManager.getLogger(Network.class.getName());

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private Callback onMessageReceivedCallback;
    private Callback onCommandReceivedCallback;
    private Callback onConnectCallback;
    private Callback onDisconnectCallback;

    public void setOnMessageReceivedCallback(Callback onMessageReceivedCallback) {
        this.onMessageReceivedCallback = onMessageReceivedCallback;
    }

    public void setOnCommandReceivedCallback(Callback onCommandReceivedCallback) {
        this.onCommandReceivedCallback = onCommandReceivedCallback;
    }

    public void setOnConnectCallback(Callback onConnectCallback) {
        this.onConnectCallback = onConnectCallback;
    }

    public void setOnDisconnectCallback(Callback onDisconnectCallback) {
        this.onDisconnectCallback = onDisconnectCallback;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    public void connect(int port) throws IOException{
        socket = new Socket("localhost", port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        if (onConnectCallback != null) {
            onConnectCallback.callback();
        }

        Thread t = new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith("/")) {
                        if (onCommandReceivedCallback != null) {
                            onCommandReceivedCallback.callback(msg);
                        }
                        break;
                    }
                    if (onMessageReceivedCallback != null){
                        onMessageReceivedCallback.callback(msg);
                    }
                }
            } catch (IOException e) {
                logger.throwing(Level.ERROR, e);
            } finally {
                disconnect();
            }
        });
        t.start();
    }

    public void sendMessage(String msg) throws IOException {
        out.writeUTF(msg);
    }

    public void tryToLogin(String login, String password) throws IOException {
        sendMessage("/login " + login + " " + password);
    }

    public void disconnect(){
        if (onDisconnectCallback != null) {
            onDisconnectCallback.callback();
        }
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            logger.throwing(Level.ERROR, e);
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.throwing(Level.ERROR, e);
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.throwing(Level.ERROR, e);
        }
    }

}

package ru.ArtemSmirnov.java2.chat.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DbQueryProvider {
    private static final Logger logger = LogManager.getLogger(DbQueryProvider.class.getName());

    private Connection connection;
    private PreparedStatement preparedStatement;

    public DbQueryProvider(Connection connection) {
        this.connection = connection;
    }

    public void registerServer() {
        try {
            preparedStatement = connection.prepareStatement("insert into running_instances(hostname, start_date) values(?,?);");
            preparedStatement.setString(1, System.getenv().get("COMPUTERNAME"));
            preparedStatement.setString(2, getCurrentTime());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.throwing(Level.ERROR, e);
        } finally {
            disconnect();
        }
    }

    public void registerClient(ClientHandler clientHandler) {
        try {
            preparedStatement = connection.prepareStatement("insert into client_sessions(user_id, start_date) values(?, ?);");
            preparedStatement.setInt(1, clientHandler.getUserId());
            preparedStatement.setString(2, getCurrentTime());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.throwing(Level.ERROR, e);
        } finally {
            disconnect();
        }
    }

    public void unregisterClient(ClientHandler clientHandler) {
        try {
            preparedStatement = connection.prepareStatement("update client_sessions set end_date = ? where user_id = ?;");
            preparedStatement.setString(1, getCurrentTime());
            preparedStatement.setInt(2, clientHandler.getUserId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.throwing(Level.ERROR, e);
        } finally {
            disconnect();
        }
    }

    public void unregisterServer() {
        try {
            preparedStatement = connection.prepareStatement("update running_instances set end_date = ? where end_date is null;");
            preparedStatement.setString(1, getCurrentTime());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.throwing(Level.ERROR, e);
        } finally {
            disconnect();
        }
    }

    private void disconnect() {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                logger.throwing(Level.ERROR, e);
            }
        }
    }

    private String getCurrentTime() {
        Date date = new Date();
        SimpleDateFormat formatDate = new SimpleDateFormat("dd.mm.yyyyy HH:mm:ss");
        return formatDate.format(date);
    }
}

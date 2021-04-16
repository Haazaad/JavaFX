package ru.ArtemSmirnov.java2.chat.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DbConnection {
    private static final Logger logger = LogManager.getLogger(DbConnection.class.getName());

    private Connection connection;

    public Connection getConnection() {
        return connection;
    }

    public DbConnection(){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:database.db");
        } catch (ClassNotFoundException | SQLException e) {
            logger.throwing(Level.ERROR, e);
            throw new RuntimeException("Невозможно подключиться к БД");
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.throwing(Level.ERROR, e);
            }
        }
    }
}

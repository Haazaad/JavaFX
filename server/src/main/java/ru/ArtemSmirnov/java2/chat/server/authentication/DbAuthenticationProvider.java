package ru.ArtemSmirnov.java2.chat.server.authentication;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;

public class DbAuthenticationProvider implements AuthenticationProvider{
    private static final Logger logger = LogManager.getLogger(DbAuthenticationProvider.class.getName());

    private Connection connection;
    private PreparedStatement preparedStatement;

    public DbAuthenticationProvider(Connection connection) {
        this.connection = connection;
    }

    private void prepareAuthRequest() throws SQLException {
        preparedStatement = connection.prepareStatement("select u.id, c.nickname\n" +
                "from Users u\n" +
                "join Credentials c on u.id = c.user_id\n" +
                "where u.login = ?\n" +
                "and c.pass = ?;");
    }

    private void prepareChangeNicknameRequest() throws SQLException {
        preparedStatement = connection.prepareStatement("update credentials set nickname = ? where user_id = ?;");
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

    @Override
    public ArrayList<String> getCredentialsByLoginAndPassword(String login, String password) {
        ArrayList<String> credentials = new ArrayList<>();
        try {
            prepareAuthRequest();
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            try (ResultSet result = preparedStatement.executeQuery()) {
                while (result.next()) {
                    credentials.add(result.getString(1));
                    credentials.add(result.getString(2));
                }
            }
        } catch (SQLException e) {
            logger.throwing(Level.ERROR, e);
        } finally {
            disconnect();
        }
        return credentials;
    }

    public boolean isUserOnline(String username) {
        String query = String.format("select cs.user_id from credentials c, client_sessions cs\n" +
                "where c.user_id = cs.user_id\n" +
                "and cs.end_date is null\n" +
                "and c.nickname = '%s'", username);
        try {
            Statement stmt = connection.createStatement();
            try (ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.throwing(Level.ERROR, e);
        }
        return false;
    }

    @Override
    public void changeNickname(int userId, String newNickname) {
        try {
            prepareChangeNicknameRequest();
            preparedStatement.setString(1, newNickname);
            preparedStatement.setInt(2, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.throwing(Level.ERROR, e);
        } finally {
            disconnect();
        }
    }
}

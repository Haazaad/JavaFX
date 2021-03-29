package ru.ArtemSmirnov.java2.chat.server.authentication;

import java.sql.*;
import java.util.ArrayList;

public class DbAuthenticationProvider implements AuthenticationProvider{
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
                e.printStackTrace();
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
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return credentials;
    }

    @Override
    public void changeNickname(int userId, String newNickname) {
        try {
            prepareChangeNicknameRequest();
            preparedStatement.setString(1, newNickname);
            preparedStatement.setInt(2, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }
}

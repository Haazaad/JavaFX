package ru.ArtemSmirnov.java2.chat.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Сделал такую версию хранения авторизационных данных для пользователей.
 * Вся проверка и валидация проводится сервером через метод validateUser().
 * Просьба прокомментировать правильность подхода
 */
public class Users {
    private Map<String, Credentials> listOfUsers;

    private class Credentials {
        Map<String, String> credentials;

        private Credentials(String password, String username) {
            credentials = new HashMap<>();
            credentials.put(password, username);
        }

        private boolean passwordIsValid(String password) {
            return credentials.containsKey(password);
        }

        private String getUsername(String password) {
            return credentials.get(password);
        }
    }

    public Users() {
        listOfUsers = new HashMap<>();
        listOfUsers.put("1111", new Credentials("1111", "Bob"));
        listOfUsers.put("2222", new Credentials("2222", "Jack"));
        listOfUsers.put("3333", new Credentials("3333", "Max"));
        listOfUsers.put("4444", new Credentials("4444", "John"));
    }

    public boolean havingUser(String login) {
        return listOfUsers.containsKey(login);
    }

    public String returnUsername(String login, String password) {
        if (!listOfUsers.get(login).passwordIsValid(password)) {
            return null;
        }
        return listOfUsers.get(login).getUsername(password);
    }
}

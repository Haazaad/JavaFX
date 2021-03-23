package ru.ArtemSmirnov.java2.chat.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InMemoryAuthenticationProvider implements AuthenticationProvider {

    private class User {
        private String login;
        private String password;
        private String nickname;

        public User(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    private List<User> users;

    public InMemoryAuthenticationProvider() {
        this.users = new ArrayList<>(Arrays.asList(
                new User("1111", "1111", "Bob"),
                new User("2222", "2222", "Jack"),
                new User("3333", "3333", "Max"),
                new User("4444", "4444", "John")
        ));
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
       for (User u: users) {
           if (u.login.equals(login) && u.password.equals(password)) {
               return u.nickname;
           }
       }
       return null;
    }

    @Override
    public void changeNickname(String oldNickname, String newNickname) {
        for (User u: users) {
            if (u.nickname.equals(oldNickname)) {
                u.nickname = newNickname;
                return;
            }
        }
    }
}

package ru.ArtemSmirnov.java2.chat.server.authentication;

import java.util.ArrayList;

public interface AuthenticationProvider {
    ArrayList<String> getCredentialsByLoginAndPassword(String login, String password);
    void changeNickname(int userId, String newNickname);
}

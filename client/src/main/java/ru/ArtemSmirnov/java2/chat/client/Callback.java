package ru.ArtemSmirnov.java2.chat.client;

@FunctionalInterface
public interface Callback {
    void callback(Object... args);
}

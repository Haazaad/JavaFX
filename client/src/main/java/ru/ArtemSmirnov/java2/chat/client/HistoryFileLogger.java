package ru.ArtemSmirnov.java2.chat.client;

import java.io.*;

public class HistoryFileLogger {
    private File logFile;

    public HistoryFileLogger(String username) {
        logFile = new File(username + ".log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String readFile() {
        StringBuilder builder = new StringBuilder();
        try (InputStreamReader in = new InputStreamReader(new BufferedInputStream(new FileInputStream(logFile)), "UTF-8")) {
            int x;
            while ((x = in.read()) != -1) {
                builder.append((char) x);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public void writeFile(String message) {
        try (OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(logFile, true)), "UTF-8"))  {
            out.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void renameFile(String username) {
        logFile.renameTo(new File(username + ".log"));
    }
}
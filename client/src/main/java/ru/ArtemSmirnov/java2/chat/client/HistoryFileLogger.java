package ru.ArtemSmirnov.java2.chat.client;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class HistoryFileLogger {
    private static final Logger logger = LogManager.getLogger(HistoryFileLogger.class.getName());

    private String username;
    private File logFile;
    private OutputStreamWriter out;

    public void init(String username) {
        try {
            this.username = username;
            out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(getFilename(), true)), "UTF-8");
        } catch (IOException e) {
            logger.throwing(Level.ERROR, e);
        }
    }

    public String readFile() {
        StringBuilder builder = new StringBuilder();
        try (InputStreamReader in = new InputStreamReader(new BufferedInputStream(new FileInputStream(getFilename())), "UTF-8")) {
            int x;
            while ((x = in.read()) != -1) {
                builder.append((char) x);
            }
        } catch (IOException e) {
            logger.throwing(Level.ERROR, e);
        }
        return builder.toString();
    }

    public void writeFile(String message) {
        try {
            out.write(message);
        } catch (IOException e) {
            logger.throwing(Level.ERROR, e);
        }
    }

    public void renameFile(String username) {
        logFile.renameTo(new File("/history_" + username + ".txt"));
    }

    public void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                logger.throwing(Level.ERROR, e);
            }
        }
    }

    private String getFilename() {
        return "history/history_" + username + ".txt";
    }
}

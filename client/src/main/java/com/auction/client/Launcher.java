package com.auction.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Launcher {
    public static void main(String[] args) {
        // #region agent log
        try {
            Path log = Paths.get(System.getProperty("user.dir")).normalize().resolve("debug-438ab6.log");
            String line =
                    "{\"sessionId\":\"438ab6\",\"runId\":\"post-fix-verify\",\"hypothesisId\":\"JPMS\",\"location\":\"Launcher.java\",\"message\":\"Launcher started after JPMS fix\",\"data\":{\"user.dir\":\""
                            + System.getProperty("user.dir").replace("\\", "/").replace("\"", "\\\"")
                            + "\"},\"timestamp\":"
                            + System.currentTimeMillis()
                            + "}\n";
            Files.writeString(log, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
        // #endregion
        MainApp.main(args);
    }
}
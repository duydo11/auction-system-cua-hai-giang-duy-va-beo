package com.auction.client;

public class Launcher {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Unhandled runtime error on " + thread.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace(System.err);
        });
        MainApp.main(args);
    }
}

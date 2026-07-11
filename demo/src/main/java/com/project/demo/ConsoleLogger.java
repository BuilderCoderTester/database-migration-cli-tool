package com.project.demo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConsoleLogger {

    // ANSI Escape Codes for Console Colors
    private static final String RESET = "\u001B[0m";
    private static final String TEXT_CYAN = "\u001B[36m";   // Info
    private static final String TEXT_GREEN = "\u001B[32m";  // Success
    private static final String TEXT_YELLOW = "\u001B[33m"; // Warning
    private static final String TEXT_RED = "\u001B[31m";    // Error

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static String getTimestamp() {
        return LocalDateTime.now().format(formatter);
    }

    public static void info(String message) {
        System.out.println(TEXT_CYAN + "[" + getTimestamp() + "] [INFO] " + message + RESET);
    }

    public static void success(String message) {
        System.out.println(TEXT_GREEN + "[" + getTimestamp() + "] [SUCCESS] " + message + RESET);
    }

    public static void warning(String message) {
        System.out.println(TEXT_YELLOW + "[" + getTimestamp() + "] [WARNING] " + message + RESET);
    }

    public static void error(String message) {
        System.err.println(TEXT_RED + "[" + getTimestamp() + "] [ERROR] " + message + RESET);
    }
}
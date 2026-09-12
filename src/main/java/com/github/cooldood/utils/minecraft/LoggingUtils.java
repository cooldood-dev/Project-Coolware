package com.github.cooldood.utils.minecraft;

public class LoggingUtils {
    public static void sendChatMessage(String message) {
        ChatUtil.prefixMessage(message);
    }
}

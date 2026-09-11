package com.github.cooldood.bridge.net.minecraft.util;

public interface SessionBridge {
    static SessionBridge from(Object instance) {
        return (SessionBridge) instance;
    }
}
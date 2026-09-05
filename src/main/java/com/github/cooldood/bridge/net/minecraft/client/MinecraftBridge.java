package com.github.cooldood.bridge.net.minecraft.client;

import com.github.cooldood.bridge.net.minecraft.util.SessionBridge;
import com.github.cooldood.bridge.net.minecraft.util.TimerBridge;

public interface MinecraftBridge {
    static MinecraftBridge from(Object instance) {
        return (MinecraftBridge) instance;
    }

    TimerBridge bridge$getTimer();
    void bridge$setSession(SessionBridge session);

    void bridge$clickMouse();
    void bridge$rightClickMouse();
    void bridge$sendClickBlockToController(boolean leftClick);
}
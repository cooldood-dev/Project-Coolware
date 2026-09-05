package com.github.cooldood.bridge.net.minecraft.util;

public interface TimerBridge {
    static TimerBridge from(Object instance) {
        return (TimerBridge) instance;
    }

    float bridge$getTimerSpeed();
    void bridge$setTimerSpeed(float timerSpeed);

    float bridge$getTickDelta();
}
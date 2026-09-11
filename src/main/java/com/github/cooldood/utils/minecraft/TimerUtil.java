package com.github.cooldood.utils.minecraft;

import com.github.cooldood.bridge.net.minecraft.client.MinecraftBridge;
import com.github.cooldood.utils.client.C;

import java.util.ArrayList;

public class TimerUtil {
    public static final ArrayList<Float> timerSpeeds = new ArrayList<>();

    public static float getTimer() {
        return MinecraftBridge.from(C.mc).bridge$getTimer().bridge$getTimerSpeed();
    }

    public static void pushTimer(float timerSpeed) {
        timerSpeeds.add(timerSpeed);
        setTimer(timerSpeed);
    }

    public static void popTimer(float timerSpeed) {
        timerSpeeds.remove(timerSpeed);
        setTimer(timerSpeeds.isEmpty() ? 1F :timerSpeeds.get(timerSpeeds.size() - 1));
    }

    private static void setTimer(float timerSpeed) {
        MinecraftBridge.from(C.mc).bridge$getTimer().bridge$setTimerSpeed(timerSpeed);
    }

    public static float getTickDelta() {
        return MinecraftBridge.from(C.mc).bridge$getTimer().bridge$getTickDelta();
    }
}

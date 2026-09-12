package com.github.cooldood.utils.minecraft;

public class TimerUtils {
    private long lastMS = System.currentTimeMillis();

    public void reset() {
        lastMS = System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(long ms, boolean useMS) {
        if (System.currentTimeMillis() - lastMS >= ms) {
            if (useMS) reset();
            return true;
        }
        return false;
    }

    public boolean hasTimeElapsed(long ms) {
        return hasTimeElapsed(ms, false);
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }

    public void setTime(long time) {
        this.lastMS = time;
    }
}

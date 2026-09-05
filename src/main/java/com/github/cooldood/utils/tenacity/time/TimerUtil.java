package com.github.cooldood.utils.tenacity.time;


public class TimerUtil {

    public long lastMS = System.currentTimeMillis();

    public TimerUtil() {
    }

    public TimerUtil(long initialMS) {
        this.lastMS = initialMS;
    }

    public void reset() {
        lastMS = System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(long time, boolean reset) {
        if (System.currentTimeMillis() - lastMS > time) {
            if (reset) reset();
            return true;
        }

        return false;
    }

    public boolean hasTimeElapsed(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }

    public boolean hasTimeElapsed(double time) {
        return hasTimeElapsed((long) time);
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }

    public void setTime(long time) {
        lastMS = time;
    }

    // ---- Chronometer-compatible aliases (merged from utils.backtrack.Chronometer) ----

    public long getElapsed() {
        return getTime();
    }

    public boolean hasElapsed() {
        return hasTimeElapsed(0L);
    }

    public boolean hasElapsed(long ms) {
        return hasTimeElapsed(ms);
    }

    /**
     * Pushes the next "elapsed" point ahead by at least {@code ms} milliseconds
     * from the current instant. Used by backtrack-style cool-downs that need to
     * forbid the next trigger for a minimum window even if the timer was already
     * counting.
     */
    public void waitForAtLeast(long ms) {
        this.lastMS = Math.max(this.lastMS, System.currentTimeMillis() + ms);
    }

}

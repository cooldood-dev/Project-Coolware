package coolware.util.math;

public class TimeUtil {
    private long lastMS;

    public TimeUtil() {
        reset();
    }

    public boolean finished(final long delay) {
        return System.currentTimeMillis() - delay >= lastMS;
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }

    public void reset() {
        this.lastMS = System.currentTimeMillis();
    }

    public long getLastMS() {
        return lastMS;
    }

    public void setLastMS(long lastMS) {
        this.lastMS = lastMS;
    }
}
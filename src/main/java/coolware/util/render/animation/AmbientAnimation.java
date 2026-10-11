package coolware.util.render.animation;

import coolware.util.math.TimeUtil;

public class AmbientAnimation {

    private final TimeUtil stopwatch = new TimeUtil();
    private Easing easing;

    private long duration;
    private double startPoint, endPoint, value;

    private boolean finished;

    public AmbientAnimation(final Easing easing, final long duration) {
        this.easing = easing;
        this.duration = duration;
    }

    public void run(final double endPoint) {
        if (this.endPoint != endPoint) {
            this.endPoint = endPoint;
            this.reset();
        } else {
            this.finished = stopwatch.finished(duration);

            if (this.finished) {
                this.value = endPoint;
                return;
            }
        }

        final double newValue = this.easing.getFunction().apply(this.getProgress());

        if (this.value > endPoint) {
            this.value = this.startPoint - (this.startPoint - endPoint) * newValue;
        } else {
            this.value = this.startPoint + (endPoint - this.startPoint) * newValue;
        }
    }

    public double getProgress() {
        return (double) (System.currentTimeMillis() - this.stopwatch.getLastMS()) / (double) this.duration;
    }

    public void reset() {
        this.stopwatch.reset();
        this.startPoint = value;
        this.finished = false;
    }

    public void restart() {
        this.stopwatch.reset();
        this.startPoint = 0.0F;
        this.finished = false;
    }

    public float getFloatValue() {
        return (float) value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public boolean isFinished() {
        return finished;
    }

    public Easing getEasing() {
        return easing;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
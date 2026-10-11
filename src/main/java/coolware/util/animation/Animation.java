package coolware.util.animation;

public abstract class Animation {

    protected long timer = System.currentTimeMillis();
    protected int duration;
    protected double endPoint;
    protected Direction direction;

    public Animation(int ms, double endPoint) {
        this(ms, endPoint, Direction.FORWARDS);
    }

    public Animation(int ms, double endPoint, Direction direction) {
        this.duration = ms;
        this.endPoint = endPoint;
        this.direction = direction;
    }

    public boolean finished(Direction direction) {
        return isDone() && this.direction.equals(direction);
    }

    public double getLinearOutput() {
        return 1.0D - (((System.currentTimeMillis() - timer) / (double) duration) * endPoint);
    }

    public double getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(double endPoint) {
        this.endPoint = endPoint;
    }

    public void reset() {
        timer = System.currentTimeMillis();
    }

    public boolean isDone() {
        return (System.currentTimeMillis() - timer) >= duration;
    }

    public void changeDirection() {
        setDirection(direction.opposite());
    }

    public Direction getDirection() {
        return direction;
    }

    public Animation setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            long elapsed = System.currentTimeMillis() - timer;
            timer = System.currentTimeMillis() - (duration - Math.min(duration, elapsed));
        }
        return this;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    protected boolean correctOutput() {
        return false;
    }

    public double getOutput() {
        if (duration <= 0) return endPoint;
        long elapsed = System.currentTimeMillis() - timer;
        if (direction.forwards()) {
            if (isDone()) {
                return endPoint;
            }
            return getEquation(elapsed / (double) duration) * endPoint;
        } else {
            if (isDone()) {
                return 0.0;
            }
            if (correctOutput()) {
                double revTime = Math.min(duration, Math.max(0, duration - elapsed));
                return getEquation(revTime / duration) * endPoint;
            }
            return (1.0 - getEquation(elapsed / (double) duration)) * endPoint;
        }
    }

    protected abstract double getEquation(double x);
}

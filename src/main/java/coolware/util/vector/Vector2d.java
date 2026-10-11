package coolware.util.vector;

public final class Vector2d {
    public double x;
    public double y;

    public Vector2d() {
    }

    public Vector2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2d offset(double x, double y) {
        return new Vector2d(this.x + x, this.y + y);
    }

    public Vector2d offset(Vector2d vector2d) {
        return this.offset(vector2d.x, vector2d.y);
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }
}

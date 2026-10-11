package coolware.util.math;

public class MathInterpolation {
    public static double interpolate(double current, double prev, double factor) {
        return prev + (current - prev) * factor;
    }

    public static float interpolate(float current, float prev, float factor) {
        return prev + (current - prev) * factor;
    }
}

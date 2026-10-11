package coolware.util.render.animation;

import java.util.function.Function;

import static java.lang.Math.sin;

public enum Easing {
    LINEAR(x -> x),
    EASE_IN_OUT_SINE(x -> -(Math.cos(Math.PI * x) - 1) / 2),
    EASE_IN_OUT_QUAD(x -> x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2),
    EASE_OUT_QUAD(x -> 1 - (1 - x) * (1 - x)),
    EASE_OUT_SINE(x -> sin(x * Math.PI / 2)),
    EASE_IN_OUT_BACK(x -> x < 0.5
            ? (Math.pow(2 * x, 2) * ((1.70158 * 1.525 + 1) * 2 * x - 1.70158 * 1.525)) / 2
            : (Math.pow(2 * x - 2, 2) * ((1.70158 * 1.525 + 1) * (x * 2 - 2) + 1.70158 * 1.525) + 2) / 2),
    EASE_OUT_BACK(x -> 1 + 2.70158 * Math.pow(x - 1, 3) + 1.70158 * Math.pow(x - 1, 2));

    private final Function<Double, Double> function;

    Easing(Function<Double, Double> function) {
        this.function = function;
    }

    public Function<Double, Double> getFunction() {
        return function;
    }
}
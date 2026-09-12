package com.github.cooldood.utils.minecraft;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.concurrent.ThreadLocalRandom;

public class MathUtils {

    public static final float PI = (float) Math.PI;
    public static final float TO_RADIANS = PI / 180.0F;
    public static final float TO_DEGREES = 180.0F / PI;

    public static double wrappedDifference(double number1, double number2) {
        return Math.min(Math.abs(number1 - number2), Math.min(Math.abs(number1 - 360) - Math.abs(number2 - 0), Math.abs(number2 - 360) - Math.abs(number1 - 0)));
    }

    public static double roundToDecimalPlace(double value, double inc) {
        final double halfOfInc = inc / 2.0D;
        final double floored = StrictMath.floor(value / inc) * inc;
        if (value >= floored + halfOfInc)
            return new BigDecimal(StrictMath.ceil(value / inc) * inc, MathContext.DECIMAL64).
                    stripTrailingZeros()
                    .doubleValue();
        else
            return new BigDecimal(floored, MathContext.DECIMAL64)
                    .stripTrailingZeros()
                    .doubleValue();
    }

    public static double getRandom(double min, double max) {
        if (min == max) {
            return min;
        } else if (min > max) {
            final double d = min;
            min = max;
            max = d;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static float getRandom(float min, float max) {
        if (min == max) {
            return min;
        } else if (min > max) {
            final float f = min;
            min = max;
            max = f;
        }
        return (float) ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static int getRandomInt(int min, int max) {
        if (min == max) {
            return min;
        } else if (min > max) {
            final int i = min;
            min = max;
            max = i;
        }
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double PI = Math.PI;
        double output = 1.0 / Math.sqrt(2.0 * PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    public static int lerp(int a, int b, float f) {
        return a + (int)(f * (float)(b - a));
    }

    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }
}

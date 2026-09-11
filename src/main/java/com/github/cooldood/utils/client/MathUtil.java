package com.github.cooldood.utils.client;

import java.util.Random;

public class MathUtil {

    private static final Random rand = new Random();

    public static double roundTo(double num, int precision) {
        double multi = Math.pow(10, precision);
        return Math.round(num * multi) / multi;
    }

    public static float roundTo(float num, int precision) {
        double multi = Math.pow(10, precision);
        return (float) (Math.round(((double) num) * multi) / multi);
    }

    public static double getRandomInRange(double min, double max) {
        if (min > max) {
            double m = min;
            min = max;
            max = m;
        }
        return min + (max - min) * rand.nextFloat();
    }

    public static float getRandomInRange(float min, float max) {
        if (min > max) {
            float m = min;
            min = max;
            max = m;
        }
        return min + (max - min) * rand.nextFloat();
    }

    public static float toNearest(float num, float nearest) {
        return Math.round(num / nearest) * nearest;
    }

    public static double toNearest(double num, double nearest) {
        return Math.round(num / nearest) * nearest;
    }
}
package com.github.cooldood.utils.minecraft;

import java.util.Random;

public class MathUtils {
    private static final Random random = new Random();

    public static double getRandom(double min, double max) {
        if (min >= max) return min;
        return min + random.nextDouble() * (max - min);
    }

    public static float getRandom(float min, float max) {
        if (min >= max) return min;
        return min + random.nextFloat() * (max - min);
    }

    public static int getRandomInt(int min, int max) {
        if (min >= max) return min;
        return min + random.nextInt(max - min + 1);
    }
}

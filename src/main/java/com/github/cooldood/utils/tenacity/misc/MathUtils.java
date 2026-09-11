package com.github.cooldood.utils.tenacity.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.text.DecimalFormat;

public class MathUtils {

    public static final DecimalFormat DF_0 = new DecimalFormat("0");
    public static final DecimalFormat DF_1 = new DecimalFormat("0.0");
    public static final DecimalFormat DF_2 = new DecimalFormat("0.00");
    public static final DecimalFormat DF_1D = new DecimalFormat("0.#");
    public static final DecimalFormat DF_2D = new DecimalFormat("0.##");

    public static final SecureRandom secureRandom = new SecureRandom();

    public static int getRandomInRange(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public static double[] yawPos(double value) {
        return yawPos(Minecraft.getMinecraft().thePlayer.rotationYaw * (float)(Math.PI / 180.0), value);
    }

    public static double[] yawPos(float yaw, double value) {
        return new double[]{-MathHelper.sin(yaw) * value, MathHelper.cos(yaw) * value};
    }

    public static float getRandomInRange(float min, float max) {
        return secureRandom.nextFloat() * (max - min) + min;
    }

    public static double getRandomInRange(double min, double max) {
        return min == max ? min : secureRandom.nextDouble() * (max - min) + min;
    }

    public static int getRandomNumberUsingNextInt(int min, int max) {
        java.util.Random random = new java.util.Random();
        return random.nextInt(max - min) + min;
    }

    public static double lerp(double old, double newVal, double amount) {
        return (1.0 - amount) * old + amount * newVal;
    }

    public static double interpolate(double oldValue, double newValue, double interpolationValue){
        return oldValue + (newValue - oldValue) * interpolationValue;
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue){
        return (float) (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue){
        return (int) (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double output = 1.0 / Math.sqrt(2.0 * Math.PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    public static double roundToHalf(double d) {
        return Math.round(d * 2) / 2.0;
    }

    /**
     * @deprecated The second argument is treated as decimal places after casting to int.
     * Use {@link #round(double, int)} for new call sites.
     */
    @Deprecated
    public static double round(double num, double increment) {
        return round(num, (int) increment);
    }

    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static String round(String value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.stripTrailingZeros();
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.toString();
    }

    public static float getRandomFloat(float max, float min) {
        return secureRandom.nextFloat() * (max - min) + min;
    }



    public static int getNumberOfDecimalPlace(double value) {
        final BigDecimal bigDecimal = new BigDecimal(value);
        return Math.max(0, bigDecimal.stripTrailingZeros().scale());
    }

}

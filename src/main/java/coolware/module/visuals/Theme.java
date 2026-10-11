package coolware.module.visuals;

import coolware.module.Category;
import coolware.module.Module;
import org.lwjgl.input.Keyboard;

public class Theme extends Module {
    private static Theme INSTANCE;

    public enum ColorMode {
        STATIC,
        GRADIENT,
        FADE
    }

    private ColorMode colorMode = ColorMode.STATIC;

    // Primary & Secondary Hex colors
    private String primaryHex = "#FF3344";   // Vibrant Crimson Red
    private String secondaryHex = "#FF7788"; // Soft Red / Coral for gradient
    private int primaryColor = 0xFFFF3344;
    private int secondaryColor = 0xFFFF7788;

    private float gradientSpeed = 1.0F;

    public Theme() {
        super("Theme", "Allows customizing the client color palette and gradient", Category.CLIENT, Keyboard.KEY_NONE);
        this.setToggled(true);
        INSTANCE = this;
    }

    public static Theme getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Theme();
        }
        return INSTANCE;
    }

    public ColorMode getColorMode() {
        return colorMode;
    }

    public void setColorMode(ColorMode colorMode) {
        this.colorMode = colorMode;
    }

    public void cycleColorMode() {
        ColorMode[] modes = ColorMode.values();
        this.colorMode = modes[(colorMode.ordinal() + 1) % modes.length];
    }

    public String getPrimaryHex() {
        return primaryHex;
    }

    public void setPrimaryHex(String hex) {
        this.primaryHex = hex;
        try {
            int parsed = parseHexColor(hex);
            this.primaryColor = 0xFF000000 | parsed;
        } catch (Exception ignored) {}
    }

    public String getSecondaryHex() {
        return secondaryHex;
    }

    public void setSecondaryHex(String hex) {
        this.secondaryHex = hex;
        try {
            int parsed = parseHexColor(hex);
            this.secondaryColor = 0xFF000000 | parsed;
        } catch (Exception ignored) {}
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(int rgb) {
        this.primaryColor = 0xFF000000 | (rgb & 0xFFFFFF);
        this.primaryHex = String.format("#%06X", (rgb & 0xFFFFFF));
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(int rgb) {
        this.secondaryColor = 0xFF000000 | (rgb & 0xFFFFFF);
        this.secondaryHex = String.format("#%06X", (rgb & 0xFFFFFF));
    }

    public float getGradientSpeed() {
        return gradientSpeed;
    }

    public void setGradientSpeed(float gradientSpeed) {
        this.gradientSpeed = gradientSpeed;
    }

    /**
     * Calculates the theme color based on current mode, time offset, and index.
     */
    public int getColor(int index) {
        if (colorMode == ColorMode.STATIC) {
            return primaryColor;
        }

        long time = (long) (System.currentTimeMillis() * gradientSpeed);
        double factor = (Math.sin((time + index * 120L) / 400.0) + 1.0) / 2.0;

        return interpolateColor(primaryColor, secondaryColor, (float) factor);
    }

    public static java.awt.Color[] getThemeColours() {
        Theme theme = getInstance();
        return new java.awt.Color[]{
                new java.awt.Color(theme.getPrimaryColor()),
                new java.awt.Color(theme.getSecondaryColor())
        };
    }

    public static int interpolateColor(int color1, int color2, float factor) {
        factor = Math.max(0.0F, Math.min(1.0F, factor));

        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;

        int r = (int) (r1 + factor * (r2 - r1));
        int g = (int) (g1 + factor * (g2 - g1));
        int b = (int) (b1 + factor * (b2 - b1));
        int a = (int) (a1 + factor * (a2 - a1));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int parseHexColor(String hex) {
        if (hex == null) return 0xFF3344;
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        if (cleaned.length() == 3) {
            StringBuilder sb = new StringBuilder();
            for (char c : cleaned.toCharArray()) {
                sb.append(c).append(c);
            }
            cleaned = sb.toString();
        }
        return (int) Long.parseLong(cleaned, 16);
    }
}

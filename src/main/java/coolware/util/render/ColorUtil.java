package coolware.util.render;

import org.lwjgl.opengl.GL11;
import java.awt.Color;

public final class ColorUtil {
    private ColorUtil() {}

    public static void glColor(int hex) {
        float a = (hex >> 24 & 0xFF) / 255.0F;
        float r = (hex >> 16 & 0xFF) / 255.0F;
        float g = (hex >> 8 & 0xFF) / 255.0F;
        float b = (hex & 0xFF) / 255.0F;
        GL11.glColor4f(r, g, b, a);
    }

    public static void glColor(Color color) {
        if (color == null) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            return;
        }
        GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
    }

    public static Color mix(Color color, Color other, double factor) {
        if (color == null) return other;
        if (other == null) return color;
        double d0 = 1.0 - factor;
        int r = (int)(color.getRed() * factor + other.getRed() * d0);
        int g = (int)(color.getGreen() * factor + other.getGreen() * d0);
        int b = (int)(color.getBlue() * factor + other.getBlue() * d0);
        return new Color(Math.min(255, Math.max(0, r)), Math.min(255, Math.max(0, g)), Math.min(255, Math.max(0, b)));
    }
}

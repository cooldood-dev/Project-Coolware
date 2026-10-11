package coolware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;

public class RoundedUtils {

    public static void drawRound(float x, float y, float width, float height, float radius, Color color) {
        if (color == null) return;
        RenderUtil.drawRoundedRect(x, y, width, height, radius, color.getRGB());
    }

    public static void drawRound(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        if (color == null) return;
        RenderUtil.drawRoundedRect(x, y, width, height, radius, color.getRGB());
    }

    public static void drawRoundOutline(float x, float y, float width, float height, float radius, float outlineThickness, Color color, Color outlineColor) {
        if (color != null && color.getAlpha() > 0) {
            RenderUtil.drawRoundedRect(x, y, width, height, radius, color.getRGB());
        }
        if (outlineColor != null && outlineColor.getAlpha() > 0) {
            RenderUtil.drawRoundedOutline(x, y, width, height, radius, Math.max(1.0F, outlineThickness), outlineColor.getRGB());
        }
    }

    public static void drawRoundTextured(float x, float y, float width, float height, float radius, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 0.0F, 0.0F, (int) width, (int) height, (int) width, (int) height, width, height);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawGradientHorizontal(float x, float y, float width, float height, float radius, Color left, Color right) {
        if (left == null || right == null) return;
        RenderUtil.drawHorizontalGradientSideways(x, y, width, height, left.getRGB(), right.getRGB());
    }

    public static void drawGradientVertical(float x, float y, float width, float height, float radius, Color top, Color bottom) {
        if (top == null || bottom == null) return;
        RenderUtil.drawRoundedGradient(x, y, width, height, radius, top.getRGB(), bottom.getRGB());
    }

    public static void drawRoundShadow(float x, float y, float width, float height, float radius, float shadowSize, Color shadowColor) {
        if (shadowColor == null || shadowColor.getAlpha() <= 0) return;
        RenderUtil.drawRoundedDropShadow(x, y, width, height, radius, shadowSize, shadowColor.getRGB());
    }
}

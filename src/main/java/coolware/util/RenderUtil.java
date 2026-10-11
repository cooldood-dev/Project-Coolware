package coolware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class RenderUtil {

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left < right) {
            float temp = left;
            left = right;
            right = temp;
        }

        if (top < bottom) {
            float temp = top;
            top = bottom;
            bottom = temp;
        }

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);

        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos((double) left, (double) bottom, 0.0D).endVertex();
        worldrenderer.pos((double) right, (double) bottom, 0.0D).endVertex();
        worldrenderer.pos((double) right, (double) top, 0.0D).endVertex();
        worldrenderer.pos((double) left, (double) top, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRectWH(float x, float y, float width, float height, int color) {
        drawRect(x, y, x + width, y + height, color);
    }

    public static void drawBorderedRect(float left, float top, float right, float bottom, float lineWidth, int borderColor, int insideColor) {
        drawRect(left, top, right, bottom, insideColor);
        drawRect(left, top, left + lineWidth, bottom, borderColor);
        drawRect(right - lineWidth, top, right, bottom, borderColor);
        drawRect(left + lineWidth, top, right - lineWidth, top + lineWidth, borderColor);
        drawRect(left + lineWidth, bottom - lineWidth, right - lineWidth, bottom, borderColor);
    }

    public static Color interpolateColors(Color color1, Color color2, double percent) {
        return new Color(
                (int) ((color1.getRed() * (1.0D - percent)) + (color2.getRed() * percent)),
                (int) ((color1.getGreen() * (1.0D - percent)) + (color2.getGreen() * percent)),
                (int) ((color1.getBlue() * (1.0D - percent)) + (color2.getBlue() * percent)),
                (int) ((color1.getAlpha() * (1.0D - percent)) + (color2.getAlpha() * percent))
        );
    }

    public static double getColorsFadeValue(double value, Color[] colours, double fadeSpeed) {
        double time = System.currentTimeMillis() * fadeSpeed * 0.001;
        double fadeValue = value * -0.001;
        return Math.abs((time + fadeValue) % colours.length);
    }

    public static Color getColorsFade(double value, Color[] colours, double fadeSpeed) {
        if (colours == null || colours.length == 0) return new Color(255, 255, 255);
        if (colours.length == 1) return colours[0];
        double fadeTimeStart = getColorsFadeValue(value, colours, fadeSpeed);

        int idx1 = (int) fadeTimeStart % colours.length;
        int idx2 = (idx1 + 1) % colours.length;

        return interpolateColors(
                colours[idx1],
                colours[idx2],
                fadeTimeStart % 1.0D
        );
    }

    public static Color setOpacity(Color color, double opacity) {
        int alpha = (int) Math.max(0, Math.min(255, opacity * 255.0D));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        boolean wasCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        if (wasCull) GL11.glDisable(GL11.GL_CULL_FACE);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        if (wasDepth) GlStateManager.disableDepth();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_POLYGON);

        // Top Right Corner
        for (int i = 0; i <= 90; i += 6) {
            double ang = Math.toRadians(i);
            GL11.glVertex2d(x + width - radius + Math.sin(ang) * radius, y + radius - Math.cos(ang) * radius);
        }

        // Bottom Right Corner
        for (int i = 90; i <= 180; i += 6) {
            double ang = Math.toRadians(i);
            GL11.glVertex2d(x + width - radius + Math.sin(ang) * radius, y + height - radius - Math.cos(ang) * radius);
        }

        // Bottom Left Corner
        for (int i = 180; i <= 270; i += 6) {
            double ang = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(ang) * radius, y + height - radius - Math.cos(ang) * radius);
        }

        // Top Left Corner
        for (int i = 270; i <= 360; i += 6) {
            double ang = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(ang) * radius, y + radius - Math.cos(ang) * radius);
        }

        GL11.glEnd();

        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        if (wasDepth) GlStateManager.enableDepth();
        if (wasCull) GL11.glEnable(GL11.GL_CULL_FACE);
    }

    public static void drawRoundedOutline(float x, float y, float width, float height, float radius, float lineWidth, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        boolean wasCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        if (wasCull) GL11.glDisable(GL11.GL_CULL_FACE);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        if (wasDepth) GlStateManager.disableDepth();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_LINE_LOOP);

        // Top Right Corner
        for (int i = 0; i <= 90; i += 6) {
            double ang = Math.toRadians(i);
            GL11.glVertex2d(x + width - radius + Math.sin(ang) * radius, y + radius - Math.cos(ang) * radius);
        }

        // Bottom Right Corner
        for (int i = 90; i <= 180; i += 6) {
            double ang = Math.toRadians(i);
            GL11.glVertex2d(x + width - radius + Math.sin(ang) * radius, y + height - radius - Math.cos(ang) * radius);
        }

        // Bottom Left Corner
        for (int i = 180; i <= 270; i += 6) {
            double ang = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(ang) * radius, y + height - radius - Math.cos(ang) * radius);
        }

        // Top Left Corner
        for (int i = 270; i <= 360; i += 6) {
            double ang = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.sin(ang) * radius, y + radius - Math.cos(ang) * radius);
        }

        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        if (wasDepth) GlStateManager.enableDepth();
        if (wasCull) GL11.glEnable(GL11.GL_CULL_FACE);
    }

    public static void drawRoundedGradient(float x, float y, float width, float height, float radius, int topColor, int bottomColor) {
        float a1 = (float) (topColor >> 24 & 255) / 255.0F;
        float r1 = (float) (topColor >> 16 & 255) / 255.0F;
        float g1 = (float) (topColor >> 8 & 255) / 255.0F;
        float b1 = (float) (topColor & 255) / 255.0F;

        float a2 = (float) (bottomColor >> 24 & 255) / 255.0F;
        float r2 = (float) (bottomColor >> 16 & 255) / 255.0F;
        float g2 = (float) (bottomColor >> 8 & 255) / 255.0F;
        float b2 = (float) (bottomColor & 255) / 255.0F;

        boolean wasCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        if (wasCull) GL11.glDisable(GL11.GL_CULL_FACE);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        if (wasDepth) GlStateManager.disableDepth();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_POLYGON);

        // Top Right Corner
        for (int i = 0; i <= 90; i += 6) {
            double ang = Math.toRadians(i);
            float py = (float) (y + radius - Math.cos(ang) * radius);
            float factor = (py - y) / height;
            GL11.glColor4f(r1 + (r2 - r1) * factor, g1 + (g2 - g1) * factor, b1 + (b2 - b1) * factor, a1 + (a2 - a1) * factor);
            GL11.glVertex2d(x + width - radius + Math.sin(ang) * radius, py);
        }

        // Bottom Right Corner
        for (int i = 90; i <= 180; i += 6) {
            double ang = Math.toRadians(i);
            float py = (float) (y + height - radius - Math.cos(ang) * radius);
            float factor = (py - y) / height;
            GL11.glColor4f(r1 + (r2 - r1) * factor, g1 + (g2 - g1) * factor, b1 + (b2 - b1) * factor, a1 + (a2 - a1) * factor);
            GL11.glVertex2d(x + width - radius + Math.sin(ang) * radius, py);
        }

        // Bottom Left Corner
        for (int i = 180; i <= 270; i += 6) {
            double ang = Math.toRadians(i);
            float py = (float) (y + height - radius - Math.cos(ang) * radius);
            float factor = (py - y) / height;
            GL11.glColor4f(r1 + (r2 - r1) * factor, g1 + (g2 - g1) * factor, b1 + (b2 - b1) * factor, a1 + (a2 - a1) * factor);
            GL11.glVertex2d(x + radius + Math.sin(ang) * radius, py);
        }

        // Top Left Corner
        for (int i = 270; i <= 360; i += 6) {
            double ang = Math.toRadians(i);
            float py = (float) (y + radius - Math.cos(ang) * radius);
            float factor = (py - y) / height;
            GL11.glColor4f(r1 + (r2 - r1) * factor, g1 + (g2 - g1) * factor, b1 + (b2 - b1) * factor, a1 + (a2 - a1) * factor);
            GL11.glVertex2d(x + radius + Math.sin(ang) * radius, py);
        }

        GL11.glEnd();

        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        if (wasDepth) GlStateManager.enableDepth();
        if (wasCull) GL11.glEnable(GL11.GL_CULL_FACE);
    }

    public static void drawCircle(float cx, float cy, float radius, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 360; i += 10) {
            double ang = Math.toRadians(i);
            GL11.glVertex2d(cx + Math.sin(ang) * radius, cy + Math.cos(ang) * radius);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawCircleOutline(float cx, float cy, float radius, float lineWidth, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 360; i += 10) {
            double ang = Math.toRadians(i);
            GL11.glVertex2d(cx + Math.sin(ang) * radius, cy + Math.cos(ang) * radius);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawSmoothLine(float x1, float y1, float x2, float y2, float width, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(width);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2d(x1, y1);
        GL11.glVertex2d(x2, y2);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawSoftGlow(float x, float y, float width, float height, float radius, int glowColor, int layers) {
        int baseAlpha = (glowColor >> 24) & 0xFF;
        int rgb = glowColor & 0x00FFFFFF;

        for (int i = 1; i <= layers; i++) {
            float spread = i * 2.0F;
            int layerAlpha = (int) ((float) baseAlpha / (layers * 1.5F + i));
            int col = (layerAlpha << 24) | rgb;
            drawRoundedOutline(x - spread, y - spread, width + spread * 2, height + spread * 2, radius + spread, 2.0F, col);
        }
    }

    public static void drawHorizontalGradient(float left, float top, float right, float bottom, int startColor, int endColor) {
        float f = (float) (startColor >> 24 & 255) / 255.0F;
        float f1 = (float) (startColor >> 16 & 255) / 255.0F;
        float f2 = (float) (startColor >> 8 & 255) / 255.0F;
        float f3 = (float) (startColor & 255) / 255.0F;

        float f4 = (float) (endColor >> 24 & 255) / 255.0F;
        float f5 = (float) (endColor >> 16 & 255) / 255.0F;
        float f6 = (float) (endColor >> 8 & 255) / 255.0F;
        float f7 = (float) (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos((double) left, (double) top, 0.0D).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos((double) left, (double) bottom, 0.0D).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos((double) right, (double) bottom, 0.0D).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos((double) right, (double) top, 0.0D).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawGradientRect(float left, float top, float right, float bottom, int startColor, int endColor) {
        float f = (float) (startColor >> 24 & 255) / 255.0F;
        float f1 = (float) (startColor >> 16 & 255) / 255.0F;
        float f2 = (float) (startColor >> 8 & 255) / 255.0F;
        float f3 = (float) (startColor & 255) / 255.0F;

        float f4 = (float) (endColor >> 24 & 255) / 255.0F;
        float f5 = (float) (endColor >> 16 & 255) / 255.0F;
        float f6 = (float) (endColor >> 8 & 255) / 255.0F;
        float f7 = (float) (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos((double) right, (double) top, 0.0D).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos((double) left, (double) top, 0.0D).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos((double) left, (double) bottom, 0.0D).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos((double) right, (double) bottom, 0.0D).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawTexture(ResourceLocation texture, float x, float y, float width, float height, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.01F);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(r, g, b, a);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos((double) x, (double) (y + height), 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos((double) (x + width), (double) (y + height), 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos((double) (x + width), (double) y, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos((double) x, (double) y, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    public static void prepareScissor(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        int scissorY = mc.displayHeight - (y + height) * scale;
        int scissorX = x * scale;
        int scissorW = width * scale;
        int scissorH = height * scale;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void drawPlayTriangle(float cx, float cy, float size, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);

        float half = size / 2.0F;
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2d(cx - half * 0.7F, cy - half);
        GL11.glVertex2d(cx - half * 0.7F, cy + half);
        GL11.glVertex2d(cx + half * 0.9F, cy);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawMicrosoftIcon(float cx, float cy, float size, int color) {
        float half = size / 2.0F;
        float gap = size * 0.12F;
        float tileSize = (size - gap) / 2.0F;

        // Top-left tile
        drawRoundedRect(cx - half, cy - half, tileSize, tileSize, 1.2F, color);
        // Top-right tile
        drawRoundedRect(cx - half + tileSize + gap, cy - half, tileSize, tileSize, 1.2F, color);
        // Bottom-left tile
        drawRoundedRect(cx - half, cy - half + tileSize + gap, tileSize, tileSize, 1.2F, color);
        // Bottom-right tile
        drawRoundedRect(cx - half + tileSize + gap, cy - half + tileSize + gap, tileSize, tileSize, 1.2F, color);
    }

    public static void drawPlusIcon(float cx, float cy, float size, float strokeWidth, int color) {
        float half = size / 2.0F;
        drawSmoothLine(cx - half, cy, cx + half, cy, strokeWidth, color);
        drawSmoothLine(cx, cy - half, cx, cy + half, strokeWidth, color);
    }

    public static void drawUserSolidIcon(float cx, float cy, float size, int color) {
        // user.svg: solid head circle and solid rounded shoulders
        drawCircle(cx, cy - size * 0.22F, size * 0.25F, color);
        drawRoundedRect(cx - size * 0.44F, cy + size * 0.08F, size * 0.88F, size * 0.42F, size * 0.20F, color);
    }

    public static void drawClipboardUserIcon(float cx, float cy, float size, int color) {
        // clipboard-user.svg: clipboard outline, top clip, and user profile avatar inside
        float w = size * 0.80F;
        float h = size * 0.96F;
        drawRoundedOutline(cx - w / 2.0F, cy - h / 2.0F, w, h, 2.8F, 2.0F, color);
        drawRoundedRect(cx - 3.5F, cy - h / 2.0F - 2.0F, 7.0F, 3.5F, 1.2F, color);

        // Inner User
        drawCircle(cx, cy - h * 0.10F, size * 0.15F, color);
        drawRoundedRect(cx - size * 0.24F, cy + h * 0.12F, size * 0.48F, size * 0.24F, 2.0F, color);
    }

    public static void drawRefreshIcon(float cx, float cy, float size, int color) {
        // refresh.svg: circular loop with directional arrowheads
        float r = size * 0.38F;
        float a = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float grn = (float) (color >> 8 & 255) / 255.0F;
        float blu = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, grn, blu, a);
        GL11.glLineWidth(2.2F);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 25; i <= 305; i += 10) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(cx + Math.cos(rad) * r, cy + Math.sin(rad) * r);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();

        // Arrow head at 305 degrees (top-right)
        double tipRad = Math.toRadians(305);
        float tipX = (float) (cx + Math.cos(tipRad) * r);
        float tipY = (float) (cy + Math.sin(tipRad) * r);
        drawSmoothLine(tipX - 1.0F, tipY - 4.8F, tipX + 1.2F, tipY, 2.2F, color);
        drawSmoothLine(tipX + 4.8F, tipY - 1.0F, tipX + 1.2F, tipY, 2.2F, color);
    }

    public static void drawCookieBiteIcon(float cx, float cy, float size, int color) {
        // cookie-bite.svg: round cookie body with bite cutout on top right and chocolate chip dots
        float r = size * 0.44F;
        float a = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float grn = (float) (color >> 8 & 255) / 255.0F;
        float blu = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, grn, blu, a);
        GL11.glLineWidth(2.2F);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 70; i <= 300; i += 10) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(cx + Math.cos(rad) * r, cy + Math.sin(rad) * r);
        }
        for (int i = 300; i <= 430; i += 15) {
            double rad = Math.toRadians(i - 180);
            float biteCenterX = cx + r * 0.65F;
            float biteCenterY = cy - r * 0.65F;
            float biteR = r * 0.50F;
            GL11.glVertex2d(biteCenterX + Math.cos(rad) * biteR, biteCenterY + Math.sin(rad) * biteR);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();

        // Chocolate chip dots
        drawCircle(cx - r * 0.38F, cy - r * 0.15F, 1.5F, color);
        drawCircle(cx - r * 0.12F, cy + r * 0.42F, 1.5F, color);
        drawCircle(cx + r * 0.32F, cy + r * 0.28F, 1.5F, color);
    }

    public static void drawClipboardIcon(float cx, float cy, float size, int color) {
        float w = size * 0.85F;
        float h = size * 1.05F;
        drawRoundedOutline(cx - w / 2.0F, cy - h / 2.0F, w, h, 2.5F, 2.0F, color);
        drawRoundedRect(cx - 3.5F, cy - h / 2.0F - 2.0F, 7.0F, 3.5F, 1.2F, color);
        drawSmoothLine(cx - w * 0.25F, cy - h * 0.10F, cx + w * 0.25F, cy - h * 0.10F, 1.8F, color);
        drawSmoothLine(cx - w * 0.25F, cy + h * 0.12F, cx + w * 0.25F, cy + h * 0.12F, 1.8F, color);
    }

    public static void drawRandomDiceIcon(float cx, float cy, float size, int color) {
        float half = size / 2.0F;
        drawRoundedOutline(cx - half, cy - half, size, size, 3.0F, 2.0F, color);
        float d = size * 0.25F;
        drawCircle(cx, cy, 1.3F, color);
        drawCircle(cx - d, cy - d, 1.3F, color);
        drawCircle(cx + d, cy - d, 1.3F, color);
        drawCircle(cx - d, cy + d, 1.3F, color);
        drawCircle(cx + d, cy + d, 1.3F, color);
    }

    public static void drawTrashIcon(float cx, float cy, float size, int color) {
        float w = size * 0.8F;
        float h = size * 0.9F;
        drawSmoothLine(cx - w * 0.55F, cy - h * 0.45F, cx + w * 0.55F, cy - h * 0.45F, 2.0F, color);
        drawRoundedRect(cx - 2.5F, cy - h * 0.45F - 2.5F, 5.0F, 2.5F, 1.0F, color);
        drawRoundedOutline(cx - w * 0.42F, cy - h * 0.40F, w * 0.84F, h * 0.85F, 2.0F, 2.0F, color);
        drawSmoothLine(cx - 2.5F, cy - h * 0.25F, cx - 2.5F, cy + h * 0.30F, 1.6F, color);
        drawSmoothLine(cx + 2.5F, cy - h * 0.25F, cx + 2.5F, cy + h * 0.30F, 1.6F, color);
    }

    public static void drawDirectKeyIcon(float cx, float cy, float size, int color) {
        drawCircleOutline(cx - size * 0.22F, cy, size * 0.25F, 2.0F, color);
        drawSmoothLine(cx, cy, cx + size * 0.45F, cy, 2.2F, color);
        drawSmoothLine(cx + size * 0.25F, cy, cx + size * 0.25F, cy + size * 0.22F, 2.0F, color);
        drawSmoothLine(cx + size * 0.42F, cy, cx + size * 0.42F, cy + size * 0.22F, 2.0F, color);
    }

    public static void drawCookieIcon(float cx, float cy, float size, int color) {
        drawCookieBiteIcon(cx, cy, size, color);
    }

    public static void drawChevronRight(float cx, float cy, float size, float strokeWidth, int color) {
        float half = size * 0.5F;
        float w = size * 0.42F;
        drawSmoothLine(cx - w, cy - half, cx + w, cy, strokeWidth, color);
        drawSmoothLine(cx + w, cy, cx - w, cy + half, strokeWidth, color);
    }

    public static void drawSettings3110Icon(float cx, float cy, float radius, int color) {
        // settings-3110.svg: 8-toothed mechanical cog/gear icon with hollow central opening
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);

        float rInner = radius * 0.36F; // Central hollow circular hole
        float rRoot = radius * 0.72F;  // Base radius between teeth
        float rTip = radius * 1.05F;   // Outer tooth tip radius

        int teeth = 8;
        float step = 360.0F / teeth;

        // Draw outer 8-toothed gear body ring
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= teeth; i++) {
            float base = i * step;

            // 1. Root / Valley start
            double a1 = Math.toRadians(base - 14.0F);
            GL11.glVertex2d(cx + Math.sin(a1) * rRoot, cy - Math.cos(a1) * rRoot);
            GL11.glVertex2d(cx + Math.sin(a1) * rInner, cy - Math.cos(a1) * rInner);

            // 2. Tooth tip left
            double a2 = Math.toRadians(base - 8.5F);
            GL11.glVertex2d(cx + Math.sin(a2) * rTip, cy - Math.cos(a2) * rTip);
            GL11.glVertex2d(cx + Math.sin(a2) * rInner, cy - Math.cos(a2) * rInner);

            // 3. Tooth tip right
            double a3 = Math.toRadians(base + 8.5F);
            GL11.glVertex2d(cx + Math.sin(a3) * rTip, cy - Math.cos(a3) * rTip);
            GL11.glVertex2d(cx + Math.sin(a3) * rInner, cy - Math.cos(a3) * rInner);

            // 4. Root / Valley end
            double a4 = Math.toRadians(base + 14.0F);
            GL11.glVertex2d(cx + Math.sin(a4) * rRoot, cy - Math.cos(a4) * rRoot);
            GL11.glVertex2d(cx + Math.sin(a4) * rInner, cy - Math.cos(a4) * rInner);

            // 5. Valley mid
            double a5 = Math.toRadians(base + step * 0.5F);
            GL11.glVertex2d(cx + Math.sin(a5) * rRoot, cy - Math.cos(a5) * rRoot);
            GL11.glVertex2d(cx + Math.sin(a5) * rInner, cy - Math.cos(a5) * rInner);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();

        // Inner circle outline for smooth hole edge
        drawCircleOutline(cx, cy, rInner, 1.2F, color);
    }

    public static void drawGearIcon(float cx, float cy, float radius, int color) {
        drawSettings3110Icon(cx, cy, radius, color);
    }

    public static void drawPowerIcon(float cx, float cy, float radius, float strokeWidth, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(strokeWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 40; i <= 320; i += 12) {
            double rad = Math.toRadians(i - 90);
            GL11.glVertex2d(cx + Math.cos(rad) * radius, cy + Math.sin(rad) * radius);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();

        drawSmoothLine(cx, cy - radius * 1.15F, cx, cy - radius * 0.05F, strokeWidth, color);
    }

    public static void drawGamepadIcon(float cx, float cy, float size, int color) {
        float w = size * 1.05F;
        float h = size * 0.65F;
        float rad = h * 0.42F;
        drawRoundedOutline(cx - w / 2.0F, cy - h / 2.0F, w, h, rad, 2.2F, color);

        float dX = cx - w * 0.25F;
        drawSmoothLine(dX - 3.8F, cy, dX + 3.8F, cy, 2.0F, color);
        drawSmoothLine(dX, cy - 3.8F, dX, cy + 3.8F, 2.0F, color);

        float aX = cx + w * 0.25F;
        drawCircle(aX - 2.8F, cy, 1.4F, color);
        drawCircle(aX + 2.8F, cy, 1.4F, color);
        drawCircle(aX, cy - 2.8F, 1.4F, color);
        drawCircle(aX, cy + 2.8F, 1.4F, color);
    }

    public static void drawUsersIcon(float cx, float cy, float size, int color) {
        drawCircle(cx - size * 0.10F, cy - size * 0.22F, size * 0.25F, color);
        drawRoundedRect(cx - size * 0.45F, cy + size * 0.06F, size * 0.70F, size * 0.38F, size * 0.18F, color);

        drawCircle(cx + size * 0.34F, cy - size * 0.16F, size * 0.19F, color);
        drawRoundedRect(cx + size * 0.16F, cy + size * 0.08F, size * 0.42F, size * 0.30F, size * 0.14F, color);
    }

    public static void drawIdBadgeIcon(float cx, float cy, float size, int color) {
        float w = size * 0.90F;
        float h = size * 1.10F;
        drawRoundedOutline(cx - w / 2.0F, cy - h / 2.0F, w, h, 3.5F, 2.0F, color);

        drawRoundedRect(cx - 3.5F, cy - h / 2.0F - 2.5F, 7.0F, 3.5F, 1.5F, color);

        drawCircle(cx, cy - h * 0.18F, size * 0.18F, color);
        drawRoundedRect(cx - size * 0.26F, cy + h * 0.02F, size * 0.52F, size * 0.22F, 2.0F, color);

        drawSmoothLine(cx - w * 0.28F, cy + h * 0.30F, cx + w * 0.28F, cy + h * 0.30F, 1.8F, color);
    }

    public static void drawProfileIcon(float cx, float cy, float size, int color) {
        drawUserSolidIcon(cx, cy, size, color);
    }

    public static void drawClockIcon(float cx, float cy, float radius, int color) {
        drawCircleOutline(cx, cy, radius, 2.0F, color);
        drawCircle(cx, cy, 1.5F, color);
        drawSmoothLine(cx, cy, cx, cy - radius * 0.55F, 2.0F, color);
        drawSmoothLine(cx, cy, cx + radius * 0.45F, cy, 2.0F, color);
    }

    public static void drawCalendarIcon(float cx, float cy, float size, int color) {
        float w = size * 0.95F;
        float h = size * 0.90F;
        drawRoundedOutline(cx - w / 2.0F, cy - h / 2.0F, w, h, 3.0F, 2.0F, color);
        drawSmoothLine(cx - w / 2.0F, cy - h * 0.16F, cx + w / 2.0F, cy - h * 0.16F, 2.0F, color);
        drawSmoothLine(cx - w * 0.25F, cy - h / 2.0F - 2.5F, cx - w * 0.25F, cy - h / 2.0F + 2.5F, 2.0F, color);
        drawSmoothLine(cx + w * 0.25F, cy - h / 2.0F - 2.5F, cx + w * 0.25F, cy - h / 2.0F + 2.5F, 2.0F, color);
        drawCircle(cx - w * 0.22F, cy + h * 0.12F, 1.4F, color);
        drawCircle(cx, cy + h * 0.12F, 1.4F, color);
        drawCircle(cx + w * 0.22F, cy + h * 0.12F, 1.4F, color);
    }

    public static void drawExhiRect(float x, float y, float x2, float y2, float alpha) {
        x2 = x + x2;
        y2 = y + y2;
        net.minecraft.client.gui.Gui.drawRect((int)(x - 3.5F), (int)(y - 3.5F), (int)(x2 + 3.5F), (int)(y2 + 3.5F), new Color(0, 0, 0, (int)(alpha * 255.0F)).getRGB());
        net.minecraft.client.gui.Gui.drawRect((int)(x - 3.0F), (int)(y - 3.0F), (int)(x2 + 3.0F), (int)(y2 + 3.0F), new Color(50F / 255F, 50F / 255F, 50F / 255F, alpha).getRGB());
        net.minecraft.client.gui.Gui.drawRect((int)(x - 2.5F), (int)(y - 2.5F), (int)(x2 + 2.5F), (int)(y2 + 2.5F), new Color(26F / 255F, 26F / 255F, 26F / 255F, alpha).getRGB());
        net.minecraft.client.gui.Gui.drawRect((int)(x - 0.5F), (int)(y - 0.5F), (int)(x2 + 0.5F), (int)(y2 + 0.5F), new Color(50F / 255F, 50F / 255F, 50F / 255F, alpha).getRGB());
        net.minecraft.client.gui.Gui.drawRect((int)x, (int)y, (int)x2, (int)y2, new Color(18F / 255F, 18F / 255F, 18F / 255F, alpha).getRGB());
    }

    public static long startTime = System.currentTimeMillis();

    public static String sessionTime() {
        int elapsedTime = (int) ((System.currentTimeMillis() - startTime) / 1000L);
        String days = elapsedTime > 86400 ? (elapsedTime / 86400) + "d " : "";
        elapsedTime = !days.isEmpty() ? elapsedTime % 86400 : elapsedTime;
        String hours = elapsedTime > 3600 ? (elapsedTime / 3600) + "h " : "";
        elapsedTime = !hours.isEmpty() ? elapsedTime % 3600 : elapsedTime;
        String minutes = elapsedTime > 60 ? (elapsedTime / 60) + "m " : "";
        elapsedTime = !minutes.isEmpty() ? elapsedTime % 60 : elapsedTime;
        String seconds = elapsedTime > 0 ? elapsedTime + "s" : "";
        return days + hours + minutes + seconds;
    }

    public static String sessionTime2() {
        int elapsedTime = (int) ((System.currentTimeMillis() - startTime) / 1000L);
        return elapsedTime > 60 ? (elapsedTime / 60) + " minutes " : "0 minutes ";
    }

    public static void drawGradientRect(final double left, final double top, double right, double bottom, final boolean sideways, final int startColor, final int endColor) {
        right = left + right;
        bottom = top + bottom;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        if (sideways) {
            color(startColor);
            GL11.glVertex2d(left, top);
            GL11.glVertex2d(left, bottom);
            color(endColor);
            GL11.glVertex2d(right, bottom);
            GL11.glVertex2d(right, top);
        } else {
            color(startColor);
            GL11.glVertex2d(left, top);
            color(endColor);
            GL11.glVertex2d(left, bottom);
            GL11.glVertex2d(right, bottom);
            color(startColor);
            GL11.glVertex2d(right, top);
        }
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    public static void color(int color) {
        float f = (float)(color >> 24 & 255) / 255.0F;
        float f1 = (float)(color >> 16 & 255) / 255.0F;
        float f2 = (float)(color >> 8 & 255) / 255.0F;
        float f3 = (float)(color & 255) / 255.0F;
        GlStateManager.color(f1, f2, f3, f);
    }

    public static void drawHorizontalGradientSideways(double x, double y, double width, double height, int leftColor, int rightColor) {
        drawGradientRect(x, y, width, height, true, leftColor, rightColor);
    }

    public static void scissor(double x, double y, double width, double height) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor = sr.getScaleFactor();
        GL11.glScissor((int) (x * scaleFactor),
                (int) (mc.displayHeight - (y + height) * scaleFactor),
                (int) (width * scaleFactor), (int) (height * scaleFactor));
    }

    public static void drawBorder(float x, float y, float width, float height, float outlineThickness, int outlineColor) {
        float a = (float) (outlineColor >> 24 & 255) / 255.0F;
        float r = (float) (outlineColor >> 16 & 255) / 255.0F;
        float g = (float) (outlineColor >> 8 & 255) / 255.0F;
        float b = (float) (outlineColor & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.color(r, g, b, a);

        GL11.glLineWidth(outlineThickness);
        float cornerValue = outlineThickness * 0.19F;

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2d(x, y - cornerValue);
        GL11.glVertex2d(x, y + height + cornerValue);
        GL11.glVertex2d(x + width, y + height + cornerValue);
        GL11.glVertex2d(x + width, y - cornerValue);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x + width, y);
        GL11.glVertex2d(x, y + height);
        GL11.glVertex2d(x + width, y + height);
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawBorderedRectDim(float x, float y, float width, float height, float outlineThickness, int rectColor, int outlineColor) {
        drawRect(x, y, x + width, y + height, rectColor);
        drawBorder(x, y, width, height, outlineThickness, outlineColor);
    }

    public static void drawGoodCircle(double x, double y, float radius, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glPointSize(radius * (2 * Minecraft.getMinecraft().gameSettings.guiScale));
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2d(x, y);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void renderPlayer2D(net.minecraft.entity.EntityLivingBase entity, float x, float y, float size, float radius, int color) {
        if (!(entity instanceof net.minecraft.client.entity.AbstractClientPlayer)) return;
        net.minecraft.client.entity.AbstractClientPlayer player = (net.minecraft.client.entity.AbstractClientPlayer) entity;

        if (radius > 0) {
            StencilUtils.initStencilToWrite();
            drawRoundedRect(x, y, size, size, radius, -1);
            StencilUtils.readStencilBuffer(1);
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Minecraft.getMinecraft().getTextureManager().bindTexture(player.getLocationSkin());
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        GlStateManager.color(r, g, b, a);
        net.minecraft.client.gui.Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 8.0F, 8.0F, 8, 8, (int) size, (int) size, 64.0F, 64.0F);
        net.minecraft.client.gui.Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 40.0F, 8.0F, 8, 8, (int) size, (int) size, 64.0F, 64.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        if (radius > 0) {
            StencilUtils.uninitStencilBuffer();
        }
    }

    public static void renderPlayer2D(net.minecraft.client.entity.AbstractClientPlayer player, float x, float y, float size, float radius, int color) {
        renderPlayer2D((net.minecraft.entity.EntityLivingBase) player, x, y, size, radius, color);
    }

    public static void drawEntityOnScreen(float yaw, float pitch, net.minecraft.entity.EntityLivingBase entityLivingBase) {
        if (entityLivingBase == null || entityLivingBase.isDead) return;
        GlStateManager.resetColor();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        float renderYawOffset = entityLivingBase.renderYawOffset;
        float rotationYaw = entityLivingBase.rotationYaw;
        float rotationPitch = entityLivingBase.rotationPitch;
        float prevRotationYawHead = entityLivingBase.prevRotationYawHead;
        float rotationYawHead = entityLivingBase.rotationYawHead;
        try {
            GlStateManager.translate(0.0f, 0.0f, 50.0f);
            GlStateManager.scale(-50.0f, 50.0f, 50.0f);
            GlStateManager.rotate(180.0f, 0.0f, 0.0f, 1.0f);
            GlStateManager.rotate(135.0f, 0.0f, 1.0f, 0.0f);
            net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
            GlStateManager.rotate(-135.0f, 0.0f, 1.0f, 0.0f);
            GlStateManager.rotate((float)(-Math.atan(pitch / 40.0f) * 20.0), 1.0f, 0.0f, 0.0f);
            entityLivingBase.renderYawOffset = yaw - 0.4f;
            entityLivingBase.rotationYaw = yaw - 0.2f;
            entityLivingBase.rotationPitch = pitch;
            entityLivingBase.rotationYawHead = entityLivingBase.rotationYaw;
            entityLivingBase.prevRotationYawHead = entityLivingBase.rotationYaw;
            GlStateManager.translate(0.0f, 0.0f, 0.0f);
            net.minecraft.client.renderer.entity.RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
            if (renderManager != null) {
                renderManager.setPlayerViewY(180.0f);
                renderManager.setRenderShadow(false);
                renderManager.renderEntityWithPosYaw(entityLivingBase, 0.0, 0.0, 0.0, 0.0f, 1.0f);
                renderManager.setRenderShadow(true);
            }
        } catch (Throwable t) {
            // Ignore rendering errors to prevent crash
        } finally {
            entityLivingBase.renderYawOffset = renderYawOffset;
            entityLivingBase.rotationYaw = rotationYaw;
            entityLivingBase.rotationPitch = rotationPitch;
            entityLivingBase.prevRotationYawHead = prevRotationYawHead;
            entityLivingBase.rotationYawHead = rotationYawHead;
            GlStateManager.popMatrix();
            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit);
            GlStateManager.resetColor();
        }
    }

    public static void drawEntityOnScreen(float posX, float posY, float scale, net.minecraft.entity.EntityLivingBase ent) {
        if (ent == null || ent.isDead) return;
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.translate(posX, posY, 50.0F);
            GlStateManager.scale(-scale, scale, scale);
            GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
            net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
            GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, 0.0F, 0.0F);
            final net.minecraft.client.renderer.entity.RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
            if (rendermanager != null) {
                rendermanager.setPlayerViewY(1F);
                rendermanager.setRenderShadow(false);
                rendermanager.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
                rendermanager.setRenderShadow(true);
            }
        } catch (Throwable t) {
            // Ignore rendering errors
        } finally {
            GlStateManager.popMatrix();
            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit);
            GlStateManager.resetColor();
        }
    }

    public static void renderItemStack(net.minecraft.item.ItemStack stack, double x, double y, float scale) {
        renderItemStack(stack, x, y, scale, false, scale);
    }

    public static void renderItemStack(net.minecraft.item.ItemStack stack, double x, double y, float scale, boolean enchantedText) {
        renderItemStack(stack, x, y, scale, enchantedText, scale);
    }

    public static void renderItemStack(net.minecraft.item.ItemStack stack, double x, double y, float scale, boolean enchantedText, float textScale) {
        if (stack == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0D);
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
        if (enchantedText) {
            renderEnchantText(stack, 0, 0, textScale);
        }
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void renderItemStack(net.minecraft.entity.player.EntityPlayer target, float x, float y, float scale, boolean enchantedText, float textScale, boolean bg, boolean info) {
        if (target == null) return;
        java.util.List<net.minecraft.item.ItemStack> items = new java.util.ArrayList<net.minecraft.item.ItemStack>();
        if (target.getHeldItem() != null) {
            items.add(target.getHeldItem());
        }
        for (int index = 3; index >= 0; index--) {
            net.minecraft.item.ItemStack stack = target.inventory.armorInventory[index];
            if (stack != null) {
                items.add(stack);
            }
        }
        float i = x;
        for (net.minecraft.item.ItemStack stack : items) {
            if (bg) {
                drawRect(i, y, i + 16 * scale, y + 16 * scale, new Color(0, 0, 0, 150).getRGB());
            }
            if (info) {
                int damage = stack.getMaxDamage() - stack.getItemDamage();
                coolware.font.moonlight.Fonts.interRegular.get(16 / 2f * scale).drawCenteredStringWithShadow(damage + "", i + (16 * scale) / 2, (y + 16 + 2) * scale, -1);
            }
            renderItemStack(stack, i, y, scale, enchantedText, textScale);
            i += 16;
        }
    }

    public static void renderItemStack(net.minecraft.entity.player.EntityPlayer target, float x, float y, float scale, boolean bg, boolean info) {
        renderItemStack(target, x, y, scale, false, 0, bg, info);
    }

    public static void renderItemStack(net.minecraft.entity.player.EntityPlayer target, float x, float y, float scale, float textScale) {
        renderItemStack(target, x, y, scale, true, textScale, false, false);
    }

    public static void renderItemStack(net.minecraft.entity.player.EntityPlayer target, float x, float y, float scale) {
        renderItemStack(target, x, y, scale, scale);
    }

    public static void renderEnchantText(net.minecraft.item.ItemStack stack, double x, double y, float scale) {
        if (stack == null) return;
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        double height = y;
        if (stack.getItem() instanceof net.minecraft.item.ItemArmor) {
            int protectionLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.protection.effectId, stack);
            int unBreakingLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.unbreaking.effectId, stack);
            int thornLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.thorns.effectId, stack);
            if (protectionLevel > 0) {
                drawEnchantTag("P" + getEnchantColor(protectionLevel) + protectionLevel, x, height, scale);
                height += 8 * scale;
            }
            if (unBreakingLevel > 0) {
                drawEnchantTag("U" + getEnchantColor(unBreakingLevel) + unBreakingLevel, x, height, scale);
                height += 8 * scale;
            }
            if (thornLevel > 0) {
                drawEnchantTag("T" + getEnchantColor(thornLevel) + thornLevel, x, height, scale);
                height += 8 * scale;
            }
        }
        if (stack.getItem() instanceof net.minecraft.item.ItemBow) {
            int powerLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.power.effectId, stack);
            int punchLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.punch.effectId, stack);
            int flameLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.flame.effectId, stack);
            int unBreakingLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.unbreaking.effectId, stack);
            if (powerLevel > 0) {
                drawEnchantTag("Pow" + getEnchantColor(powerLevel) + powerLevel, x, height, scale);
                height += 8 * scale;
            }
            if (punchLevel > 0) {
                drawEnchantTag("Pun" + getEnchantColor(punchLevel) + punchLevel, x, height, scale);
                height += 8 * scale;
            }
            if (flameLevel > 0) {
                drawEnchantTag("F" + getEnchantColor(flameLevel) + flameLevel, x, height, scale);
                height += 8 * scale;
            }
            if (unBreakingLevel > 0) {
                drawEnchantTag("U" + getEnchantColor(unBreakingLevel) + unBreakingLevel, x, height, scale);
                height += 8 * scale;
            }
        }
        if (stack.getItem() instanceof net.minecraft.item.ItemSword) {
            int sharpnessLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.sharpness.effectId, stack);
            int knockBackLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.knockback.effectId, stack);
            int fireAspectLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.fireAspect.effectId, stack);
            int unBreakingLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.unbreaking.effectId, stack);
            if (sharpnessLevel > 0) {
                drawEnchantTag("S" + getEnchantColor(sharpnessLevel) + sharpnessLevel, x, height, scale);
                height += 8 * scale;
            }
            if (knockBackLevel > 0) {
                drawEnchantTag("K" + getEnchantColor(knockBackLevel) + knockBackLevel, x, height, scale);
                height += 8 * scale;
            }
            if (fireAspectLevel > 0) {
                drawEnchantTag("F" + getEnchantColor(fireAspectLevel) + fireAspectLevel, x, height, scale);
                height += 8 * scale;
            }
            if (unBreakingLevel > 0) {
                drawEnchantTag("U" + getEnchantColor(unBreakingLevel) + unBreakingLevel, x, height, scale);
                height += 8 * scale;
            }
        }
        if (stack.getRarity() == net.minecraft.item.EnumRarity.EPIC) {
            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();
            GL11.glTranslated(x, height, 0);
            GL11.glScaled(scale, scale, scale);
            coolware.font.moonlight.Fonts.interRegular.get(14).drawStringWithShadow("God", 0, 0, new Color(255, 255, 0).getRGB());
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }

    private static void drawEnchantTag(String text, double x, double y, double scale) {
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GL11.glTranslated(x, y, 0);
        GL11.glScaled(scale, scale, scale);
        coolware.font.moonlight.Fonts.interRegular.get(14).drawStringWithShadow(text, 0, 0, -1);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private static String getEnchantColor(int n) {
        if (n != 1) {
            if (n == 2) return "§a";
            if (n == 3) return "§3";
            if (n == 4) return "§4";
            if (n >= 5) return "§e";
        }
        return "§f";
    }

    public static void resetColor() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawTexturedRect(float x, float y, float textureX, float textureY, float width, float height) {
        float f = 0.00390625F;
        float f1 = 0.00390625F;
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos((double) x, (double) (y + height), 0.0D).tex((double) (textureX * f), (double) ((textureY + height) * f1)).endVertex();
        worldrenderer.pos((double) (x + width), (double) (y + height), 0.0D).tex((double) ((textureX + width) * f), (double) ((textureY + height) * f1)).endVertex();
        worldrenderer.pos((double) (x + width), (double) y, 0.0D).tex((double) ((textureX + width) * f), (double) (textureY * f1)).endVertex();
        worldrenderer.pos((double) x, (double) y, 0.0D).tex((double) (textureX * f), (double) (textureY * f1)).endVertex();
        tessellator.draw();
    }

    public static void drawRoundedDropShadow(float x, float y, float width, float height, float radius, float shadowSize, int shadowColor) {
        float a = (float) (shadowColor >> 24 & 255) / 255.0F;
        float r = (float) (shadowColor >> 16 & 255) / 255.0F;
        float g = (float) (shadowColor >> 8 & 255) / 255.0F;
        float b = (float) (shadowColor & 255) / 255.0F;
        int steps = Math.max(1, (int) (shadowSize * 2.0F));
        for (int i = 0; i < steps; i++) {
            float progress = (float) (i + 1) / (float) steps;
            float spread = shadowSize * progress;
            float alpha = a * (1.0F - (float) Math.pow(progress, 0.6)) * 0.35F;
            int stepColor = new java.awt.Color(r, g, b, Math.max(0.0F, Math.min(1.0F, alpha))).getRGB();
            drawRoundedRect(x - spread, y - spread + (spread * 0.35F), width + spread * 2.0F, height + spread * 2.0F, radius + spread, stepColor);
        }
    }

    private static final net.minecraft.client.renderer.culling.Frustum FRUSTUM = new net.minecraft.client.renderer.culling.Frustum();

    public static boolean isInViewFrustrum(net.minecraft.entity.Entity entity) {
        if (entity == null) return false;
        return isInViewFrustrum(entity.getEntityBoundingBox()) || entity.ignoreFrustumCheck;
    }

    public static boolean isInViewFrustrum(net.minecraft.util.AxisAlignedBB box) {
        if (box == null) return false;
        net.minecraft.entity.Entity entity = net.minecraft.client.Minecraft.getMinecraft().getRenderViewEntity();
        if (entity == null) return false;
        FRUSTUM.setPosition(entity.posX, entity.posY, entity.posZ);
        return FRUSTUM.isBoundingBoxInFrustum(box);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        drawRoundedRect(x, y, width, height, radius, color.getRGB());
    }

    public static void drawCircleLoading(float x, float y, float size, float percentage, Color color) {
        drawCircleLoading(x, y, size, percentage, color, color);
    }

    public static void drawCircleLoading(float x, float y, float size, float percentage, Color color1, Color color2) {
        float r1 = color1.getRed() / 255.0F;
        float g1 = color1.getGreen() / 255.0F;
        float b1 = color1.getBlue() / 255.0F;
        float a1 = color1.getAlpha() / 255.0F;

        float r2 = color2.getRed() / 255.0F;
        float g2 = color2.getGreen() / 255.0F;
        float b2 = color2.getBlue() / 255.0F;
        float a2 = color2.getAlpha() / 255.0F;

        float cx = x + size / 2.0F;
        float cy = y + size / 2.0F;
        float outerRadius = size / 2.0F * 0.65F;
        float innerRadius = size / 2.0F * 0.50F;
        float lineWidth = outerRadius - innerRadius;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(Math.max(1.0F, lineWidth));

        float midRadius = (innerRadius + outerRadius) / 2.0F;
        float angleLimit = Math.max(0.0F, Math.min(1.0F, percentage)) * 360.0F;

        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (float deg = 0; deg <= angleLimit; deg += 3.0F) {
            float rad = (float) Math.toRadians(deg - 90.0F);
            float factor = (deg / 360.0F);
            float r = r1 + (r2 - r1) * factor;
            float g = g1 + (g2 - g1) * factor;
            float b = b1 + (b2 - b1) * factor;
            float a = a1 + (a2 - a1) * factor;
            GL11.glColor4f(r, g, b, a);
            GL11.glVertex2f(cx + (float) Math.cos(rad) * midRadius, cy + (float) Math.sin(rad) * midRadius);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawEntity3DBox(net.minecraft.entity.Entity entity, float partialTicks, Color outlineColor, Color fillColor, float lineWidth) {
        if (entity == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderManager() == null) return;

        double renderPosX = mc.getRenderManager().renderPosX;
        double renderPosY = mc.getRenderManager().renderPosY;
        double renderPosZ = mc.getRenderManager().renderPosZ;

        double dX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
        double dY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
        double dZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;

        net.minecraft.util.AxisAlignedBB bb = entity.getEntityBoundingBox();
        if (bb == null) return;

        net.minecraft.util.AxisAlignedBB renderBB = new net.minecraft.util.AxisAlignedBB(
                bb.minX - entity.posX + dX - renderPosX,
                bb.minY - entity.posY + dY - renderPosY,
                bb.minZ - entity.posZ + dZ - renderPosZ,
                bb.maxX - entity.posX + dX - renderPosX,
                bb.maxY - entity.posY + dY - renderPosY,
                bb.maxZ - entity.posZ + dZ - renderPosZ
        );

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        if (fillColor != null && fillColor.getAlpha() > 0) {
            float r = fillColor.getRed() / 255.0F;
            float g = fillColor.getGreen() / 255.0F;
            float b = fillColor.getBlue() / 255.0F;
            float a = fillColor.getAlpha() / 255.0F;
            GlStateManager.color(r, g, b, a);
            net.minecraft.client.renderer.RenderGlobal.drawSelectionBoundingBox(renderBB);
        }

        if (outlineColor != null && outlineColor.getAlpha() > 0) {
            GL11.glLineWidth(Math.max(1.0F, lineWidth));
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            net.minecraft.client.renderer.RenderGlobal.drawOutlinedBoundingBox(
                    renderBB,
                    outlineColor.getRed(),
                    outlineColor.getGreen(),
                    outlineColor.getBlue(),
                    outlineColor.getAlpha()
            );
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static Color getProgressColour(float percent) {
        percent = net.minecraft.util.MathHelper.clamp_float(percent, 0.0F, 1.0F);
        int progressInt = (int) (255 * net.minecraft.util.MathHelper.clamp_float((float) EasingUtil.EasingFunctions.Ease_In_Out_Sine.ease(percent), 0.0F, 1.0F));
        return new Color(255 - progressInt, progressInt, 80);
    }
}

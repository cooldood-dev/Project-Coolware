package com.github.cooldood.utils.render;

import com.github.cooldood.Main;
import com.github.cooldood.utils.client.C;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

public class IconFont {

    private static Font iconFont;
    private static final HashMap<Integer, IconTexture> iconTextures = new HashMap<>();

    // Common Font Awesome glyphs used by the client
    public static final String CLOCK = "\uF017";
    public static final String CALENDAR = "\uF073";
    public static final String HEART = "\uF004";
    public static final String CHART_BAR = "\uF080";
    public static final String CHEVRON_UP = "\uF35B";
    public static final String PLAY_CIRCLE = "\uF144";
    public static final String TROPHY_FLAG = "\uF024";
    public static final String TARGET_CIRCLE = "\uF192";
    public static final String CROSS_CLOSE = "\uF410";
    public static final String STAR = "\uF005";
    public static final String COMPASS = "\uF14E";

    private static final String GLYPHS = CLOCK + CALENDAR + HEART + CHART_BAR + CHEVRON_UP + PLAY_CIRCLE + TROPHY_FLAG + TARGET_CIRCLE + CROSS_CLOSE + STAR + COMPASS;
    private static final int X_SPACING = 8;

    private static final Graphics2D DUMMY_GRAPHICS = setAntiAliasing(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics());

    @AllArgsConstructor
    private static class IconTexture {
        public int textureID;
        public HashMap<Character, GlyphInfo> glyphBounds;
        public int width;
        public int height;
    }

    @AllArgsConstructor
    private static class GlyphInfo {
        public double u;
        public double uw;
        public int width;
    }

    static {
        loadFont();
    }

    private static void loadFont() {
        try {
            InputStream is = Main.class.getResourceAsStream("/fonts/fontawesome-regular.ttf");
            if (is != null) {
                iconFont = Font.createFont(Font.TRUETYPE_FONT, is);
            } else {
                System.err.println("Could not find /fonts/fontawesome-regular.ttf in resources!");
                iconFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
            }
        } catch (Exception e) {
            System.err.println("Failed to load Font Awesome font: " + e.getMessage());
            e.printStackTrace();
            iconFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
        }
    }

    @com.github.cooldood.events.SubscribeEvent
    public static void onWindowResize(com.github.cooldood.events.impl.WindowResizeEvent event) {
        iconTextures.clear();
    }

    private static float getScaleFactor() {
        return RenderUtil.renderSide != RenderUtil.RenderSide.World ? C.res().getScaleFactor() : 1;
    }

    private static Graphics2D setAntiAliasing(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
        return graphics;
    }

    private static Rectangle getFontBounds(Font font, String str) {
        DUMMY_GRAPHICS.setFont(font);
        return DUMMY_GRAPHICS.getFontMetrics().getStringBounds(str, DUMMY_GRAPHICS).getBounds();
    }

    private static IconTexture getIconTexture(int size) {
        IconTexture texture = iconTextures.get(size);
        if (texture != null) return texture;

        if (iconFont == null) loadFont();

        Font resizedFont = iconFont.deriveFont((float) size);
        Rectangle stringBounds = getFontBounds(resizedFont, GLYPHS);

        int textureWidth = stringBounds.width + (GLYPHS.length() * X_SPACING) + 20;
        int textureHeight = Math.max(stringBounds.height, size + 10);

        BufferedImage bufferedImage = new BufferedImage(Math.max(textureWidth, 1), Math.max(textureHeight, 1), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = setAntiAliasing(bufferedImage.createGraphics());
        graphics.setFont(resizedFont);
        graphics.setColor(Color.WHITE);

        HashMap<Character, GlyphInfo> glyphBounds = new HashMap<>();
        double x = X_SPACING;

        FontMetrics fm = graphics.getFontMetrics();
        int baseline = fm.getAscent();

        for (char c : GLYPHS.toCharArray()) {
            double width = fm.charWidth(c);
            if (width <= 0) width = size; // Fallback

            double u = x / (double) textureWidth;
            double uw = (x + width) / (double) textureWidth;
            glyphBounds.put(c, new GlyphInfo(u, uw, (int) width));

            graphics.drawString(String.valueOf(c), (int) x, baseline);

            x += width + X_SPACING;
        }

        graphics.dispose();

        DynamicTexture dynamicTexture = new DynamicTexture(bufferedImage);
        texture = new IconTexture(dynamicTexture.getGlTextureId(), glyphBounds, textureWidth, textureHeight);
        iconTextures.put(size, texture);

        return texture;
    }

    public static float drawIcon(String icon, float x, float y, int size, Color color) {
        if (icon == null || icon.isEmpty()) return 0;

        float scaleFactor = getScaleFactor();
        float originalSize = size;
        int generatedSize = (int) Math.min(size * scaleFactor, 80);

        IconTexture texture = getIconTexture(generatedSize);

        x = Math.round(x);
        y = Math.round(y);
        int fontHeight = texture.height;

        RenderUtil.beginRender();
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(texture.textureID);
        RenderUtil.beginAddingVertex(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        float totalWidth = 0;
        for (int i = 0; i < icon.length(); i++) {
            char c = icon.charAt(i);
            GlyphInfo info = texture.glyphBounds.get(c);
            if (info == null) continue;

            float u = (float) info.u;
            float uw = (float) info.uw;
            int glyphWidth = info.width;

            RenderUtil.addVertexTextureColor(totalWidth, fontHeight, color, u, 1);
            RenderUtil.addVertexTextureColor(totalWidth + glyphWidth, fontHeight, color, uw, 1);
            RenderUtil.addVertexTextureColor(totalWidth + glyphWidth, 0, color, uw, 0);
            RenderUtil.addVertexTextureColor(totalWidth, 0, color, u, 0);

            totalWidth += glyphWidth;
        }

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, 0);
        GL11.glScaled(originalSize / (double) generatedSize, originalSize / (double) generatedSize, 1);

        RenderUtil.finishRender();

        GL11.glPopMatrix();

        return totalWidth * (originalSize / (float) generatedSize);
    }

    public static int getWidth(String icon, int size) {
        if (icon == null || icon.isEmpty()) return 0;

        float scaleFactor = getScaleFactor();
        float originalSize = size;
        int generatedSize = (int) Math.min(size * scaleFactor, 80);

        IconTexture texture = getIconTexture(generatedSize);
        int totalWidth = 0;

        for (int i = 0; i < icon.length(); i++) {
            char c = icon.charAt(i);
            GlyphInfo info = texture.glyphBounds.get(c);
            if (info != null) {
                totalWidth += info.width;
            } else {
                totalWidth += size;
            }
        }

        return (int) (totalWidth * (originalSize / (double) generatedSize));
    }

    public static int getHeight(int size) {
        float scaleFactor = getScaleFactor();
        float originalSize = size;
        int generatedSize = (int) Math.min(size * scaleFactor, 80);

        IconTexture texture = getIconTexture(generatedSize);
        return (int) (texture.height * (originalSize / (double) generatedSize));
    }
}

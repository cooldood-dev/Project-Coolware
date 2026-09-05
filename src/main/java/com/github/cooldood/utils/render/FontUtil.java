package com.github.cooldood.utils.render;

import com.github.cooldood.Main;
import com.github.cooldood.bridge.net.minecraft.client.gui.FontRendererBridge;
import com.github.cooldood.utils.client.C;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

// todo: copy minecraft rendering stuff.
public class FontUtil {
    private static Fonts currentFont;

    private static final HashMap<Integer, FontTexture> fontTextures = new HashMap<>();

    public static void clearFontTextures() {
        for (FontTexture texture : fontTextures.values()) {
            if (texture != null && texture.textureID > 0) {
                try {
                    GlStateManager.deleteTexture(texture.textureID);
                } catch (Throwable ignored) {}
            }
        }
        fontTextures.clear();
    }

    @com.github.cooldood.events.SubscribeEvent
    public static void onWindowResize(com.github.cooldood.events.impl.WindowResizeEvent event) {
        clearFontTextures();
    }

    // Reusable arrays to avoid per-character Color allocations (GC pressure reduction)
    private static final Color[] SHADOW_COLOURS = new Color[] { new Color(22, 22, 22, 255), new Color(22, 22, 22, 255) };
    private static final Color[] SINGLE_COLOUR_CACHE = new Color[1];
    private static final Color[] FADE_CACHE = new Color[2];
    private static final Color[] CACHED_SHADOWS = new Color[256];
    static {
        for (int i = 0; i < 256; i++) CACHED_SHADOWS[i] = new Color(22, 22, 22, i);
    }

    private final static Graphics2D DUMMY_GRAPHICS = setAntiAliasing(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics());

    @AllArgsConstructor
    private static class FontTexture {
        public int textureID;
        public HashMap<Character, CharacterInfo> charBounds;
        public int width;
        public int height;
    }

    @AllArgsConstructor
    private static class UnrenderedCharacter {
        public char character;
        public int x;
        public Color colour;
    }

    @AllArgsConstructor
    private static class CharacterInfo {
        public double u;
        public double uw;
        public int width;
    }

    public static void setCurrentFont(Fonts font) {
        if (currentFont == font) return;
        clearFontTextures();
        currentFont = font;
    }

    private static FontTexture getFontTexture(int size) {
        FontTexture fontTexture = fontTextures.get(size);
        if (fontTexture != null) return fontTexture;

        Font resizedFont = currentFont.font.deriveFont((float) size);

        fontTexture = createFontTexture(resizedFont, getFontTextureBounds(resizedFont, LETTERS));
        fontTextures.put(size, fontTexture);

        return fontTexture;
    }

    // list by minecraft in FontRenderer.renderChar, removed all glyphs.
    private static final String LETTERS = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵž !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»πμΩ∞±≥≤÷≈°∙·√²\u0000";
    private static final String COLOUR_CODES = "0123456789abcdef";

    private static final int X_SPACING = 10;
    private static FontTexture createFontTexture(Font font, Rectangle stringBounds) {
        int textureWidth = stringBounds.width + (LETTERS.length() * X_SPACING);

        BufferedImage texture = new BufferedImage(textureWidth, stringBounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = setAntiAliasing(texture.createGraphics());
        graphics.setColor(Color.WHITE);
        graphics.drawRect(0, 0, 1, stringBounds.height);

        graphics.setFont(font);

        HashMap<Character, CharacterInfo> charBounds = new HashMap<>();
        double x = X_SPACING;
        for (char c : LETTERS.toCharArray()) {
            double width = getFontTextureBounds(font, String.valueOf(c)).getWidth();

            double u = x / textureWidth;
            double uw = (x + width) / textureWidth;
            charBounds.put(c, new CharacterInfo(u, uw, (int) width));

            graphics.drawString(String.valueOf(c), (int) x, font.getSize());

            x += width + X_SPACING;
        }

        graphics.dispose();

        return new FontTexture(new DynamicTexture(texture).getGlTextureId(), charBounds, stringBounds.width, stringBounds.height);
    }

    private static Rectangle getFontTextureBounds(Font font, String string) {
        DUMMY_GRAPHICS.setFont(font);
        // getStringBounds takes into account the antialiasing i think.
        return DUMMY_GRAPHICS.getFontMetrics().getStringBounds(string, DUMMY_GRAPHICS).getBounds();
    }

    private static Graphics2D setAntiAliasing(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        return graphics;
    }

    public static float drawStringFade(String string, float x, float y, int size, Color[] colour, double fadeSpeed, double fadeSpread, boolean dropShadow) {
        if (string.isEmpty()) return 0;
        Color[] inputColour = colour;

        float scaleFactor = getScaleFactor();
        float originalSize = size;
        int generatedSize = (int) Math.min(size * scaleFactor, 80);

        FontTexture fontTexture = getFontTexture(generatedSize);

        x = Math.round(x); y = Math.round(y);
        int fontHeight = fontTexture.height;

        RenderUtil.beginRender();
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(fontTexture.textureID);
        RenderUtil.beginAddingVertex(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        GL11.glTranslated(x, y, 0);
        double downScale = originalSize / (double) generatedSize;
        GL11.glScaled(downScale, downScale, 1);

        ArrayList<UnrenderedCharacter> unrenderedCharacters = new ArrayList<>();

        boolean randomStyle = false, boldStyle = false, strikethroughStyle = false, underlineStyle = false, italicStyle = false;

        int totalWidth = 0;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            if (c == '\u00A7') {
                if (i + 1 >= string.length()) continue;
                char colourCode = string.charAt(i + 1);
                if (COLOUR_CODES.indexOf(colourCode) != -1) {
                    Color colourCodeColour = new Color(C.mc.fontRendererObj.getColorCode(colourCode));
                    for (int j = 0; j < colour.length; j++) colour[j] = new Color(colourCodeColour.getRed(), colourCodeColour.getGreen(), colourCodeColour.getBlue(), colour[j].getAlpha());
                    randomStyle = boldStyle = strikethroughStyle = underlineStyle = italicStyle = false;
                }
                else {
                    switch (colourCode) {
                        case 'k':
                            randomStyle = true;
                            break;
                        case 'l':
                            boldStyle = true;
                            break;
                        case 'm':
                            strikethroughStyle = true;
                            break;
                        case 'n':
                            underlineStyle = true;
                            break;
                        case 'o':
                            italicStyle = true;
                            break;

                        default:
                            for (int j = 0; j < colour.length; j++) colour[j] = new Color(inputColour[j].getRed(),inputColour[j].getGreen(),inputColour[j].getBlue(),colour[j].getAlpha());
                            randomStyle = boldStyle = strikethroughStyle = underlineStyle = italicStyle = false;
                            break;
                    }
                }

                i++;
                continue;
            }

            if (randomStyle) c = scrambleCharacter(c, size);

            CharacterInfo characterBounds = fontTexture.charBounds.get(c);

            if (characterBounds == null) {
                int characterWidth = getMinecraftCharWidth(c, size);
                Color colourFade = RenderUtil.getColorsFade((x + totalWidth)*fadeSpread, colour, fadeSpeed);
                unrenderedCharacters.add(new UnrenderedCharacter(c, (int) (totalWidth / scaleFactor), colourFade));
                totalWidth += characterWidth;
                continue;
            }

            float u = (float) characterBounds.u;
            float uw = (float) characterBounds.uw;
            int characterWidth = characterBounds.width;

            RenderUtil.getColorsFade((x + totalWidth)*fadeSpread, characterWidth*fadeSpread, colour, fadeSpeed, FADE_CACHE);

            if (dropShadow) {
                SHADOW_COLOURS[0] = CACHED_SHADOWS[FADE_CACHE[0].getAlpha()];
                SHADOW_COLOURS[1] = CACHED_SHADOWS[FADE_CACHE[1].getAlpha()];
                drawCharacter(totalWidth + 1, 1, characterWidth, fontHeight, SHADOW_COLOURS, u, uw);
            }
            drawCharacter(totalWidth, 0, characterWidth, fontHeight, FADE_CACHE, u, uw);
            if (boldStyle) drawCharacter(totalWidth + 1, 0, characterWidth, fontHeight, FADE_CACHE, u, uw);

            float filledTextureUW = 1f / fontTexture.width;
            if (underlineStyle) drawCharacter(totalWidth, fontHeight * 0.8f, characterWidth, 1, FADE_CACHE, 0, filledTextureUW);
            if (strikethroughStyle) drawCharacter(totalWidth, fontHeight / 2f - 1, characterWidth, 2, FADE_CACHE, 0, filledTextureUW);

            // todo: impl italics, sounds like effort and they dont look good anyway.

            totalWidth += characterWidth;
        }
        RenderUtil.finishRender();

        for (UnrenderedCharacter unrenderedCharacter : unrenderedCharacters) {
            drawMinecraftString(String.valueOf(unrenderedCharacter.character), x + unrenderedCharacter.x, y, size / scaleFactor, unrenderedCharacter.colour, dropShadow);
        }

        return totalWidth / scaleFactor;
    }

    private static void drawCharacter(float x, float y, float w, float h, Color[] colours, float u, float uw) {
        RenderUtil.addVertexTextureColor(x, y+h, colours[0], u, 1);
        RenderUtil.addVertexTextureColor(x + w, y+h, colours[1], uw, 1);
        RenderUtil.addVertexTextureColor(x + w, y, colours[1], uw, 0);
        RenderUtil.addVertexTextureColor(x, y, colours[0], u, 0);
    }

    public static float drawString(String string, float x, float y, int size, Color colour, boolean dropShadow) {
        // Reuse cached array instead of allocating new Color[] on every drawString call
        SINGLE_COLOUR_CACHE[0] = colour;
        return drawStringFade(string, x, y, size, SINGLE_COLOUR_CACHE, 0, 0, dropShadow);
    }

    public static Random fontRandom = new Random();
    private static char scrambleCharacter(char c, int size) {
        int characterWidth = getCharWidth(c, size);

        char scrambledCharacter;
        while (getCharWidth((scrambledCharacter = LETTERS.charAt(fontRandom.nextInt(LETTERS.length()))), size) != characterWidth);

        return scrambledCharacter;
    }

    public static void drawCenteredString(String string, float x, float y, int size, Color color, boolean dropShadow) {
        drawString(string, x - getStringWidth(string, size) / 2f, y, size, color, dropShadow);
    }

    public static int getCharWidth(char c, int fontSize) {
        float scaleFactor = getScaleFactor();
        float originalSize = fontSize;
        int generatedSize = (int) Math.min(fontSize * scaleFactor, 80);

        FontTexture fontTexture = getFontTexture(generatedSize);
        if (fontTexture.charBounds.get(c) == null) return (int) (C.mc.fontRendererObj.getCharWidth(c) * (originalSize / (double) generatedSize));
        return (int) (fontTexture.charBounds.get(c).width * (originalSize / (double) generatedSize));
    }

    public static int getStringWidth(String string, int fontSize) {
        float scaleFactor = getScaleFactor();
        float originalSize = fontSize;
        int generatedSize = (int) Math.min(fontSize * scaleFactor, 80);

        int width = 0;
        FontTexture fontTexture = getFontTexture(generatedSize);

        HashMap<Character, CharacterInfo> charBounds = fontTexture.charBounds;

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            CharacterInfo characterRect = charBounds.get(c);
            if (c == '\u00A7')  i++;
            else if (characterRect == null) width += getMinecraftCharWidth(c, generatedSize);
            else width += characterRect.width;
        }

        return (int) (width * (originalSize / (double) generatedSize));
    }

    public static int getFontHeight(int fontSize) {
        float scaleFactor = getScaleFactor();
        float originalSize = fontSize;
        int generatedSize = (int) Math.min(fontSize * scaleFactor, 80);

        FontTexture fontTexture = getFontTexture(generatedSize);
        return (int) (fontTexture.height * (originalSize / (double) generatedSize));
    }

    private static float getScaleFactor() {
        return RenderUtil.renderSide != RenderUtil.RenderSide.World ? C.res().getScaleFactor() : 1;
    }

    private static int getMinecraftCharWidth(char string, double size) {
        return (int) (C.mc.fontRendererObj.getCharWidth(string) * 0.1 * size);
    }

    private static int getMinecraftStringWidth(String string, double size) {
        return (int) (C.mc.fontRendererObj.getStringWidth(string) * 0.1 * size);
    }

    private static int drawMinecraftString(String string, float x, float y, double size, Color colour, boolean dropShadow) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y + size / 3, 0);
        GL11.glScaled(0.1 * size, 0.1 * size, 1);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GlStateManager.enableAlpha();

        FontRendererBridge fontRendererBridge = FontRendererBridge.from(C.mc.fontRendererObj);
        fontRendererBridge.bridge$resetStyles();
        int i;
        if (dropShadow) {
            i = fontRendererBridge.bridge$renderString(string, 1.0F, 1.0F, colour.getRGB(), true);
            i = Math.max(i, fontRendererBridge.bridge$renderString(string, 0, 0, colour.getRGB(), false));
        } else {
            i = fontRendererBridge.bridge$renderString(string, 0, 0, colour.getRGB(), false);
        }

        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        return i;
    }

    public enum Fonts {
        DM_Sans_Bold("dmsans_bold.ttf");

        public final Font font;

        Fonts(String name) {
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 0);

            try {
                font =  Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Main.class.getResourceAsStream("/fonts/" + name)));
            } catch (Exception e) {
                System.err.println("Failed to load font: " + e.getMessage());
                e.printStackTrace();
            }

            this.font = font;
        }
    }

}
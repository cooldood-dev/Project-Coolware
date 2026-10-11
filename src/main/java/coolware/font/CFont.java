package coolware.font;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class CFont {
    protected float fontSize;
    protected Font font;
    protected boolean antiAlias;
    protected boolean fractionalMetrics;

    // Fast O(1) direct lookup arrays
    protected CharData[] asciiData = new CharData[256];
    protected CharData[] unicodeData = new CharData[4096]; // 0xF000 - 0xFFFF (FontAwesome & PUA)
    protected Map<Character, CharData> extendedData = new HashMap<Character, CharData>();

    protected DynamicTexture tex;
    protected int fontHeight = -1;
    protected int charOffset = 0;

    public CFont(Font font, boolean antiAlias, boolean fractionalMetrics) {
        this.font = font;
        this.fontSize = font.getSize2D();
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        this.tex = setupTexture(font, antiAlias, fractionalMetrics, this.asciiData, this.unicodeData, this.extendedData);
    }

    public CharData getCharData(char ch) {
        if (ch < 256) {
            return this.asciiData[ch];
        }
        if (ch >= 0xF000 && ch <= 0xFFFF) {
            return this.unicodeData[ch - 0xF000];
        }
        return this.extendedData.get(Character.valueOf(ch));
    }

    protected DynamicTexture setupTexture(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] ascii, CharData[] unicode, Map<Character, CharData> ext) {
        BufferedImage img = generateFontImage(font, antiAlias, fractionalMetrics, ascii, unicode, ext);
        try {
            DynamicTexture dynTex = new DynamicTexture(img);
            // Apply linear filtering for ultra-crisp scaling without FPS drops
            GlStateManager.bindTexture(dynTex.getGlTextureId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            return dynTex;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected BufferedImage generateFontImage(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] ascii, CharData[] unicode, Map<Character, CharData> ext) {
        int imgSize = (this.fontSize > 24.0F) ? 1024 : 512;
        BufferedImage bufferedImage = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        g.setFont(font);
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, imgSize, imgSize);
        g.setColor(Color.WHITE);

        if (antiAlias) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        if (fractionalMetrics) {
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }

        FontMetrics fontMetrics = g.getFontMetrics();
        int positionX = 0;
        int positionY = 1;
        int charHeight = 0;

        // 1. Pack ASCII Characters (0 - 255)
        for (int i = 0; i < 256; ++i) {
            char ch = (char) i;
            CharData data = new CharData();
            Rectangle dimensions = fontMetrics.getStringBounds(String.valueOf(ch), g).getBounds();
            data.width = dimensions.width + 8;
            data.height = dimensions.height;

            if (positionX + data.width >= imgSize) {
                positionX = 0;
                positionY += charHeight;
                charHeight = 0;
            }

            if (data.height > charHeight) {
                charHeight = data.height;
            }

            data.storedX = positionX;
            data.storedY = positionY;
            data.u1 = (float) positionX / (float) imgSize;
            data.v1 = (float) positionY / (float) imgSize;
            data.u2 = (float) (positionX + data.width) / (float) imgSize;
            data.v2 = (float) (positionY + data.height) / (float) imgSize;

            if (data.height > this.fontHeight) {
                this.fontHeight = data.height;
            }

            if (ascii != null && i < ascii.length) {
                ascii[i] = data;
            }
            g.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());
            positionX += data.width;
        }

        // 2. Pack Unicode glyphs (0xF000 - 0xFFFF) supported by font (FontAwesome & PUA)
        for (int cp = 0xF000; cp <= 0xFFFF; ++cp) {
            if (font.canDisplay(cp)) {
                char ch = (char) cp;
                CharData data = new CharData();
                Rectangle dimensions = fontMetrics.getStringBounds(String.valueOf(ch), g).getBounds();
                data.width = dimensions.width + 8;
                data.height = dimensions.height;

                if (positionX + data.width >= imgSize) {
                    positionX = 0;
                    positionY += charHeight;
                    charHeight = 0;
                }

                if (data.height > charHeight) {
                    charHeight = data.height;
                }

                data.storedX = positionX;
                data.storedY = positionY;
                data.u1 = (float) positionX / (float) imgSize;
                data.v1 = (float) positionY / (float) imgSize;
                data.u2 = (float) (positionX + data.width) / (float) imgSize;
                data.v2 = (float) (positionY + data.height) / (float) imgSize;

                if (data.height > this.fontHeight) {
                    this.fontHeight = data.height;
                }

                if (unicode != null && (cp - 0xF000) < unicode.length) {
                    unicode[cp - 0xF000] = data;
                } else if (ext != null) {
                    ext.put(Character.valueOf(ch), data);
                }
                g.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());
                positionX += data.width;
            }
        }

        return bufferedImage;
    }

    public void drawCharDirect(CharData data, float x, float y, WorldRenderer wr, float r, float g, float b, float a) {
        if (data == null) return;
        wr.pos(x, y, 0.0D).tex(data.u1, data.v1).color(r, g, b, a).endVertex();
        wr.pos(x, y + data.height, 0.0D).tex(data.u1, data.v2).color(r, g, b, a).endVertex();
        wr.pos(x + data.width, y + data.height, 0.0D).tex(data.u2, data.v2).color(r, g, b, a).endVertex();
        wr.pos(x + data.width, y, 0.0D).tex(data.u2, data.v1).color(r, g, b, a).endVertex();
    }

    public int getStringHeight(String text) {
        return getHeight();
    }

    public int getHeight() {
        return (this.fontHeight - 8) / 2;
    }

    public int getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        int width = 0;
        int len = text.length();
        for (int i = 0; i < len; ++i) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i < len - 1) {
                i++;
                continue;
            }
            CharData data = getCharData(c);
            if (data != null) {
                width += data.width - 8 + this.charOffset;
            }
        }
        return width / 2;
    }

    public boolean isAntiAlias() {
        return this.antiAlias;
    }

    public boolean isFractionalMetrics() {
        return this.fractionalMetrics;
    }

    public Font getFont() {
        return this.font;
    }

    public static class CharData {
        public int width;
        public int height;
        public int storedX;
        public int storedY;
        public float u1;
        public float v1;
        public float u2;
        public float v2;
    }
}

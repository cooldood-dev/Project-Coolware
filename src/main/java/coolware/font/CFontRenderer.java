package coolware.font;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CFontRenderer extends CFont {
    protected CharData[] boldAscii = new CharData[256];
    protected CharData[] boldUnicode = new CharData[4096];
    protected Map<Character, CharData> boldExtended = new HashMap<Character, CharData>();

    protected CharData[] italicAscii = new CharData[256];
    protected CharData[] italicUnicode = new CharData[4096];
    protected Map<Character, CharData> italicExtended = new HashMap<Character, CharData>();

    private final int[] colorCode = new int[32];
    private final String colorcodeIdentifiers = "0123456789abcdefklmnor";

    protected DynamicTexture texBold;
    protected DynamicTexture texItalic;

    // Fast string width cache to eliminate per-frame CPU iteration
    private final Map<String, Integer> stringWidthCache = new HashMap<String, Integer>(256);

    public CFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
        super(font, antiAlias, fractionalMetrics);
        setupMinecraftColorcodes();
    }

    public float drawStringWithShadow(String text, double x, double y, int color) {
        return drawBatched(text, x, y, color, true);
    }

    public float drawString(String text, float x, float y, int color) {
        return drawBatched(text, (double) x, (double) y, color, false);
    }

    public float drawString(String text, double x, double y, int color) {
        return drawBatched(text, x, y, color, false);
    }

    public float drawCenteredString(String text, float x, float y, int color) {
        return drawString(text, x - (float) (getStringWidth(text) / 2), y, color);
    }

    public float drawCenteredStringWithShadow(String text, float x, float y, int color) {
        return drawStringWithShadow(text, (double) (x - (float) (getStringWidth(text) / 2)), (double) y, color);
    }

    private float drawBatched(String text, double x, double y, int color, boolean dropShadow) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }

        if (color == 553648127) {
            color = 16777215;
        }

        if ((color >>> 24) == 0) {
            color |= -16777216;
        }

        float baseAlpha = (float) (color >> 24 & 255) / 255.0F;
        float baseR = (float) (color >> 16 & 255) / 255.0F;
        float baseG = (float) (color >> 8 & 255) / 255.0F;
        float baseB = (float) (color & 255) / 255.0F;

        x -= 1.0D;
        y -= 2.0D;
        double renderX = x * 2.0D;
        double renderY = y * 2.0D;

        GlStateManager.pushMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.scale(0.5D, 0.5D, 0.5D);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(this.tex.getGlTextureId());

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        int len = text.length();

        // Pass 1 (Optional Shadow): Offset by +1 pixel in scaled coordinate space, darkened color
        if (dropShadow) {
            double shadowX = renderX + 1.0D;
            double shadowY = renderY + 1.0D;
            float sR = baseR * 0.25F;
            float sG = baseG * 0.25F;
            float sB = baseB * 0.25F;

            for (int i = 0; i < len; ++i) {
                char character = text.charAt(i);
                if (character == '\u00a7' && i < len - 1) {
                    int colorIndex = colorcodeIdentifiers.indexOf(Character.toLowerCase(text.charAt(i + 1)));
                    if (colorIndex >= 0 && colorIndex < 16) {
                        int c = this.colorCode[colorIndex];
                        sR = ((float) (c >> 16 & 255) / 255.0F) * 0.25F;
                        sG = ((float) (c >> 8 & 255) / 255.0F) * 0.25F;
                        sB = ((float) (c & 255) / 255.0F) * 0.25F;
                    } else if (colorIndex == 21) {
                        sR = baseR * 0.25F;
                        sG = baseG * 0.25F;
                        sB = baseB * 0.25F;
                    }
                    ++i;
                } else {
                    CharData data = getCharData(character);
                    if (data != null) {
                        drawCharDirect(data, (float) shadowX, (float) shadowY, wr, sR, sG, sB, baseAlpha);
                        shadowX += (double) (data.width - 8 + this.charOffset);
                    }
                }
            }
        }

        // Pass 2 (Foreground Text):
        double curX = renderX;
        float r = baseR;
        float g = baseG;
        float b = baseB;

        for (int i = 0; i < len; ++i) {
            char character = text.charAt(i);
            if (character == '\u00a7' && i < len - 1) {
                int colorIndex = colorcodeIdentifiers.indexOf(Character.toLowerCase(text.charAt(i + 1)));
                if (colorIndex >= 0 && colorIndex < 16) {
                    int c = this.colorCode[colorIndex];
                    r = (float) (c >> 16 & 255) / 255.0F;
                    g = (float) (c >> 8 & 255) / 255.0F;
                    b = (float) (c & 255) / 255.0F;
                } else if (colorIndex == 21) {
                    r = baseR;
                    g = baseG;
                    b = baseB;
                }
                ++i;
            } else {
                CharData data = getCharData(character);
                if (data != null) {
                    drawCharDirect(data, (float) curX, (float) renderY, wr, r, g, b, baseAlpha);
                    curX += (double) (data.width - 8 + this.charOffset);
                }
            }
        }

        tessellator.draw();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        return (float) (curX / 2.0D);
    }

    @Override
    public int getStringWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        Integer cached = stringWidthCache.get(text);
        if (cached != null) {
            return cached.intValue();
        }

        int width = super.getStringWidth(text);
        if (stringWidthCache.size() > 500) {
            stringWidthCache.clear();
        }
        stringWidthCache.put(text, Integer.valueOf(width));
        return width;
    }

    private void setupMinecraftColorcodes() {
        for (int index = 0; index < 32; ++index) {
            int noClue = (index >> 3 & 1) * 85;
            int red = (index >> 2 & 1) * 170 + noClue;
            int green = (index >> 1 & 1) * 170 + noClue;
            int blue = (index & 1) * 170 + noClue;

            if (index == 6) {
                red += 85;
            }

            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }

            this.colorCode[index] = ((red & 255) << 16 | (green & 255) << 8 | blue & 255);
        }
    }
}

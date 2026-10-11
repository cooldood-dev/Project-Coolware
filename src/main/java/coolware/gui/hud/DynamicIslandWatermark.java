package coolware.gui.hud;

import coolware.Coolware;
import coolware.font.CFontRenderer;
import coolware.font.FontManager;
import coolware.module.visuals.HUD;
import coolware.module.visuals.Theme;
import coolware.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

public class DynamicIslandWatermark {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation FALLBACK_LOGO = new ResourceLocation("coolware/logo.png");
    private static ResourceLocation dynamicLogo = null;
    private static boolean dynamicLogoLoaded = false;

    // Animation state
    private static float currentWidth = 0.0F;
    private static float currentAlpha = 0.0F;
    private static long lastTime = -1L;
    private static boolean wasRendered = false;

    public static ResourceLocation getOrLoadLogo() {
        if (!dynamicLogoLoaded) {
            dynamicLogoLoaded = true; // Set flag immediately to prevent disk/stream polling every frame
            try {
                BufferedImage bImg = null;
                File directFile = new File("logo.png");
                if (directFile.exists()) {
                    bImg = ImageIO.read(directFile);
                }
                if (bImg == null) {
                    InputStream is = DynamicIslandWatermark.class.getResourceAsStream("/assets/coolware/logo.png");
                    if (is != null) {
                        bImg = ImageIO.read(is);
                        is.close();
                    }
                }
                if (bImg == null) {
                    InputStream is = DynamicIslandWatermark.class.getResourceAsStream("/coolware/logo.png");
                    if (is != null) {
                        bImg = ImageIO.read(is);
                        is.close();
                    }
                }
                if (bImg != null) {
                    dynamicLogo = mc.getTextureManager().getDynamicTextureLocation("coolware_island_logo_tex", new DynamicTexture(bImg));
                }
            } catch (Exception ignored) {
            }
        }
        return (dynamicLogo != null) ? dynamicLogo : FALLBACK_LOGO;
    }

    public static void render(HUD hud) {
        if (hud == null || !hud.isToggled()) {
            return;
        }

        long now = System.currentTimeMillis();
        float dt = (lastTime < 0) ? 0.016F : Math.min(0.1F, (now - lastTime) / 1000.0F);
        lastTime = now;

        Theme theme = Theme.getInstance();
        int accentRed = 0xFFFF3344;
        if (theme != null) {
            accentRed = theme.getColor(0);
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float scale = hud.getWatermarkScale();
        if (scale <= 0.01F) scale = 1.0F;

        float pillHeight = 22.0F;
        float radius = pillHeight / 2.0F; // 11px capsule radius

        boolean showLogo = hud.isWatermarkLogoEnabled();
        boolean showVersion = hud.isWatermarkVersionEnabled();
        boolean showFps = hud.isWatermarkFpsEnabled();
        boolean animated = hud.isWatermarkAnimationEnabled();
        boolean glow = hud.isWatermarkGlowEnabled();
        boolean shadow = hud.isWatermarkShadowEnabled();
        boolean blur = hud.isWatermarkBlurEnabled();

        float logoSize = 14.5F;
        float padLeft = 7.5F;
        float padRight = 8.5F;
        float itemGap = 6.5F;
        float sepWidth = 1.0F;

        // Extra Bold Typography
        CFontRenderer brandFont = FontManager.sfBold18 != null ? FontManager.sfBold18 : (FontManager.sfBold16 != null ? FontManager.sfBold16 : FontManager.sfBold14);
        CFontRenderer secFont = FontManager.sfBold16 != null ? FontManager.sfBold16 : (FontManager.sfSemiBold16 != null ? FontManager.sfSemiBold16 : FontManager.sfBold14);

        // Content Strings
        String brandText = Coolware.NAME;
        String versionText = "v" + Coolware.VERSION;
        int fps = Minecraft.getDebugFPS();
        String fpsText = fps + " FPS";

        // Width Calculations
        float brandW = (brandFont != null ? brandFont.getStringWidth(brandText) : mc.fontRendererObj.getStringWidth(brandText)) + 0.4F;
        float verW = (secFont != null ? secFont.getStringWidth(versionText) : mc.fontRendererObj.getStringWidth(versionText)) + 0.4F;
        float fpsW = (secFont != null ? secFont.getStringWidth(fpsText) : mc.fontRendererObj.getStringWidth(fpsText)) + 0.4F;

        float targetWidth = padLeft;
        if (showLogo) {
            targetWidth += logoSize;
            targetWidth += itemGap + sepWidth + itemGap;
        }
        targetWidth += brandW;
        if (showVersion) {
            targetWidth += itemGap + sepWidth + itemGap + verW;
        }
        if (showFps) {
            targetWidth += itemGap + sepWidth + itemGap + fpsW;
        }
        targetWidth += padRight;

        float minWidth = pillHeight; // Circular compact resting state

        if (!wasRendered) {
            currentWidth = minWidth;
            currentAlpha = 0.0F;
            wasRendered = true;
        }

        if (animated) {
            float speed = hud.getWatermarkAnimationSpeed();
            float factor = 1.0F - (float) Math.exp(-speed * dt);
            currentWidth += (targetWidth - currentWidth) * factor;
            currentAlpha += (1.0F - currentAlpha) * factor;
        } else {
            currentWidth = targetWidth;
            currentAlpha = 1.0F;
        }

        float drawW = Math.max(minWidth, currentWidth);
        float contentAlpha = Math.max(0.0F, Math.min(1.0F, (drawW - minWidth) / Math.max(1.0F, (targetWidth - minWidth))));

        // Top Centre Positioning
        float unscaledX = (sr.getScaledWidth() - drawW) / 2.0F;
        float unscaledY = 6.0F;

        // Scaled Geometry
        float scaledW = drawW * scale;
        float scaledH = pillHeight * scale;
        float scaledX = (sr.getScaledWidth() - scaledW) / 2.0F;
        float scaledY = 6.0F * scale;

        // 1. Optimized Blurred Background (Downsampled & FBO cache)
        if (blur) {
            IslandBlur.renderBlur(scaledX, scaledY, scaledW, scaledH, radius * scale);
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Scale transform around center
        float centerX = scaledX + scaledW / 2.0F;
        float centerY = scaledY + scaledH / 2.0F;
        GlStateManager.translate(centerX, centerY, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        GlStateManager.translate(-(unscaledX + drawW / 2.0F), -(unscaledY + pillHeight / 2.0F), 0.0F);

        float x = unscaledX;
        float y = unscaledY;

        // 2. Deep Realistic Drop Shadows
        if (shadow) {
            // Ambient deep diffuse shadow
            RenderUtil.drawSoftGlow(x, y + 2.5F, drawW, pillHeight, radius, ((int) (130 * currentAlpha) << 24), 7);
            // Directional contact shadow
            RenderUtil.drawSoftGlow(x, y + 1.2F, drawW, pillHeight, radius, ((int) (170 * currentAlpha) << 24), 3);
        }

        // 3. Ambient Red Drop Glow (if enabled)
        if (glow) {
            int glowAlpha = (int) (60 * currentAlpha);
            int glowColor = (glowAlpha << 24) | (accentRed & 0x00FFFFFF);
            RenderUtil.drawSoftGlow(x, y, drawW, pillHeight, radius, glowColor, 2);
        }

        // 4. Dark Translucent Acrylic Glass Overlay
        int bgTop = ((int) ((blur ? 185 : 220) * currentAlpha) << 24) | 0x00120D16;
        int bgBot = ((int) ((blur ? 215 : 240) * currentAlpha) << 24) | 0x0008050B;
        RenderUtil.drawRoundedGradient(x, y, drawW, pillHeight, radius, bgTop, bgBot);

        // 5. Vibrant Red Pill Outline
        int redOutlineAlpha = (int) (210 + 45 * currentAlpha);
        int redOutline = (redOutlineAlpha << 24) | (accentRed & 0x00FFFFFF);
        RenderUtil.drawRoundedOutline(x, y, drawW, pillHeight, radius, 1.5F, redOutline);

        // 6. Scissored Content Layer (Screen-space scissoring)
        RenderUtil.prepareScissor((int) Math.floor(scaledX), (int) Math.floor(scaledY), (int) Math.ceil(scaledW), (int) Math.ceil(scaledH));

        float curX = x + padLeft;
        int textBaseAlpha = (int) (255 * currentAlpha);

        // Logo (Crisp & bold, no independent background)
        if (showLogo) {
            float logoY = y + (pillHeight - logoSize) / 2.0F;
            ResourceLocation logoRes = getOrLoadLogo();

            try {
                RenderUtil.drawTexture(logoRes, curX, logoY, logoSize, logoSize, (textBaseAlpha << 24) | 0x00FFFFFF);
            } catch (Exception ignored) {
            }
            curX += logoSize;

            // Red Separator after Logo
            curX += itemGap;
            drawVerticalSeparator(curX, y, pillHeight, (int) (180 * contentAlpha), accentRed);
            curX += sepWidth + itemGap;
        }

        // Brand Text ("Coolware" - Extra Bold)
        int brandColor = (textBaseAlpha << 24) | 0x00FFFFFF;
        float brandY = y + (pillHeight - (brandFont != null ? brandFont.getHeight() : 8)) / 2.0F + 1.0F;
        drawBoldString(brandFont, brandText, curX, brandY, brandColor);
        curX += brandW;

        // Version ("v2.0" - Bold)
        if (showVersion) {
            curX += itemGap;
            drawVerticalSeparator(curX, y, pillHeight, (int) (180 * contentAlpha), accentRed);
            curX += sepWidth + itemGap;

            int verColor = ((int) (225 * contentAlpha) << 24) | 0x00CBC7D4;
            float verY = y + (pillHeight - (secFont != null ? secFont.getHeight() : 8)) / 2.0F + 1.0F;
            drawBoldString(secFont, versionText, curX, verY, verColor);
            curX += verW;
        }

        // FPS ("89 FPS" - Bold)
        if (showFps) {
            curX += itemGap;
            drawVerticalSeparator(curX, y, pillHeight, (int) (180 * contentAlpha), accentRed);
            curX += sepWidth + itemGap;

            int fpsColor = ((int) (250 * contentAlpha) << 24) | 0x00F0EDF5;
            float fpsY = y + (pillHeight - (secFont != null ? secFont.getHeight() : 8)) / 2.0F + 1.0F;
            drawBoldString(secFont, fpsText, curX, fpsY, fpsColor);
        }

        RenderUtil.endScissor();

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawBoldString(CFontRenderer font, String text, float x, float y, int color) {
        if (font != null) {
            font.drawStringWithShadow(text, x, y, color);
            font.drawString(text, x + 0.35F, y, color);
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
            mc.fontRendererObj.drawString(text, (int) (x + 0.5F), (int) y, color, false);
        }
    }

    private static void drawVerticalSeparator(float x, float y, float height, int alpha, int color) {
        if (alpha <= 0) return;
        int col = (alpha << 24) | (color & 0x00FFFFFF);
        RenderUtil.drawSmoothLine(x, y + 4.5F, x, y + height - 4.5F, 1.2F, col);
    }
}

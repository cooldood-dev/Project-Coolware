package coolware.gui.hud;

import coolware.Coolware;
import coolware.font.CFontRenderer;
import coolware.font.FontManager;
import coolware.font.moonlight.Fonts;
import coolware.module.visuals.HUD;
import coolware.module.visuals.Theme;
import coolware.util.ColorUtils;
import coolware.util.RenderUtil;
import coolware.util.RoundedUtils;
import coolware.util.shader.ShaderQueueType;
import coolware.util.shader.ShaderRenderQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.util.HashMap;
import java.util.Map;

public class HUDOverlay {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Map<Integer, CFontRenderer> scaledFonts = new HashMap<Integer, CFontRenderer>();

    private static CFontRenderer scaledFont(CFontRenderer template, int size) {
        if (template == null) return null;
        int key = (System.identityHashCode(template) * 31) + size;
        CFontRenderer scaled = scaledFonts.get(key);
        if (scaled == null) {
            scaled = new CFontRenderer(template.getFont().deriveFont((float) size), true, true);
            scaledFonts.put(key, scaled);
        }
        return scaled;
    }

    public static void render() {
        HUD hudModule = Coolware.getInstance().getModuleManager().getModule(HUD.class);
        if (hudModule == null || !hudModule.isToggled()) {
            return;
        }

        Theme theme = Theme.getInstance();
        int fontSize = hudModule.getFontSize();
        CFontRenderer boldFont = FontManager.sfBold18 == null ? null : scaledFont(FontManager.sfBold18, fontSize);
        CFontRenderer regularFont = FontManager.sfRegular14 == null ? null
                : scaledFont(FontManager.sfRegular14, Math.max(8, Math.round(fontSize * 14.0f / 18.0f)));

        // 1. Watermark Rendering
        String watermarkMode = hudModule.getWatermarkMode() != null ? hudModule.getWatermarkMode().getMode() : "Dynamic Island";

        if ("Dynamic Island".equalsIgnoreCase(watermarkMode)) {
            DynamicIslandWatermark.render(hudModule);
        } else if ("Simple".equalsIgnoreCase(watermarkMode)) {
            float x = hudModule.getWatermarkX();
            float y = hudModule.getWatermarkY();
            String name = Coolware.NAME;
            int fps = Minecraft.getDebugFPS();
            String details = " v" + Coolware.VERSION + " \u00a77[\u00a7f" + fps + " FPS\u00a77]";
            int themeColor = theme != null ? theme.getColor(0) : 0xFFFF4054;

            if (boldFont != null && regularFont != null) {
                boldFont.drawStringWithShadow(name, x, y, themeColor);
                int nameWidth = boldFont.getStringWidth(name);
                float detailsY = y + (boldFont.getHeight() - regularFont.getHeight()) / 2.0F;
                regularFont.drawStringWithShadow(details, x + nameWidth + 2, detailsY, 0xFFAAAAAA);
            } else {
                mc.fontRendererObj.drawStringWithShadow(name, (int) x, (int) y, themeColor);
                mc.fontRendererObj.drawStringWithShadow(details, (int) (x + mc.fontRendererObj.getStringWidth(name)), (int) y, 0xFFCCCCCC);
            }
        } else if ("Nursultan".equalsIgnoreCase(watermarkMode)) {
            float startX = hudModule.getWatermarkX();
            float startY = hudModule.getWatermarkY();
            int bgCol = new java.awt.Color(21, 21, 21, 190).getRGB();
            int themeCol = theme != null ? theme.getColor(0) : 0xFFFF4054;
            int dividerCol = new java.awt.Color(47, 47, 47).getRGB();
            float cardHeight = 15.0F;
            float cornerRadius = 4.0F;
            float cardSpacing = 6.0F;
            float currentX = startX;

            // 1. Branding Card (logo.png + divider + Coolware VERSION)
            String brandText = "Coolware " + Coolware.VERSION;
            float brandTextWidth = Fonts.interMedium.get(15).getStringWidth(brandText);
            float brandCardWidth = 7.0F + 10.0F + 6.0F + 1.0F + 6.0F + brandTextWidth + 7.0F;

            RoundedUtils.drawRound(currentX, startY, brandCardWidth, cardHeight, cornerRadius, new java.awt.Color(bgCol, true));
            net.minecraft.util.ResourceLocation logoRes = DynamicIslandWatermark.getOrLoadLogo();
            if (logoRes != null) {
                // Render bolder logo by drawing with sub-pixel offsets and solid white
                float logoX = currentX + 7.0F;
                float logoY = startY + 2.5F;
                RenderUtil.drawTexture(logoRes, logoX - 0.25F, logoY, 10.0F, 10.0F, -1);
                RenderUtil.drawTexture(logoRes, logoX + 0.25F, logoY, 10.0F, 10.0F, -1);
                RenderUtil.drawTexture(logoRes, logoX, logoY - 0.25F, 10.0F, 10.0F, -1);
                RenderUtil.drawTexture(logoRes, logoX, logoY + 0.25F, 10.0F, 10.0F, -1);
                RenderUtil.drawTexture(logoRes, logoX, logoY, 10.0F, 10.0F, -1);
            }
            RenderUtil.drawRect(currentX + 23.0F, startY + 3.0F, currentX + 24.0F, startY + 12.0F, dividerCol);
            Fonts.interMedium.get(15).drawString(brandText, currentX + 30.0F, startY + 5.5F, themeCol);

            currentX += brandCardWidth + cardSpacing;

            // 2. User Card (W icon + divider + Username)
            String playerName = (mc.thePlayer != null) ? mc.thePlayer.getName() : "Player";
            float playerNameWidth = Fonts.interMedium.get(15).getStringWidth(playerName);
            float userCardWidth = 7.0F + 9.0F + 6.0F + 1.0F + 6.0F + playerNameWidth + 7.0F;

            RoundedUtils.drawRound(currentX, startY, userCardWidth, cardHeight, cornerRadius, new java.awt.Color(bgCol, true));
            Fonts.nursultan.get(16).drawString("W", currentX + 7.0F, startY + 6.5F, themeCol);
            RenderUtil.drawRect(currentX + 22.0F, startY + 3.0F, currentX + 23.0F, startY + 12.0F, dividerCol);
            Fonts.interMedium.get(15).drawString(playerName, currentX + 29.0F, startY + 5.5F, -1);

            currentX += userCardWidth + cardSpacing;

            // 3. FPS Card (X icon + divider + FPS)
            int fps = Minecraft.getDebugFPS();
            String fpsText = fps + " FPS";
            float fpsTextWidth = Fonts.interMedium.get(15).getStringWidth(fpsText);
            float fpsCardWidth = 7.0F + 9.0F + 6.0F + 1.0F + 6.0F + fpsTextWidth + 7.0F;

            RoundedUtils.drawRound(currentX, startY, fpsCardWidth, cardHeight, cornerRadius, new java.awt.Color(bgCol, true));
            Fonts.nursultan.get(16).drawString("X", currentX + 7.0F, startY + 6.5F, themeCol);
            RenderUtil.drawRect(currentX + 22.0F, startY + 3.0F, currentX + 23.0F, startY + 12.0F, dividerCol);
            Fonts.interMedium.get(15).drawString(fpsText, currentX + 29.0F, startY + 5.5F, -1);

            currentX += fpsCardWidth + cardSpacing;

            // 4. Ping Card (Q icon + divider + Ping)
            int ping = 0;
            if (mc.thePlayer != null && mc.getNetHandler() != null) {
                net.minecraft.client.network.NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
                if (playerInfo != null) {
                    ping = Math.max(0, playerInfo.getResponseTime());
                }
            }
            String pingText = ping + " ms";
            float pingTextWidth = Fonts.interMedium.get(15).getStringWidth(pingText);
            float pingCardWidth = 7.0F + 9.0F + 6.0F + 1.0F + 6.0F + pingTextWidth + 7.0F;

            RoundedUtils.drawRound(currentX, startY, pingCardWidth, cardHeight, cornerRadius, new java.awt.Color(bgCol, true));
            Fonts.nursultan.get(16).drawString("Q", currentX + 7.0F, startY + 6.5F, themeCol);
            RenderUtil.drawRect(currentX + 22.0F, startY + 3.0F, currentX + 23.0F, startY + 12.0F, dividerCol);
            Fonts.interMedium.get(15).drawString(pingText, currentX + 29.0F, startY + 5.5F, -1);
        } else if ("Ambient".equalsIgnoreCase(watermarkMode)) {
            float startX = hudModule.getWatermarkX();
            float startY = hudModule.getWatermarkY();
            float scale = hudModule.getWatermarkScale();

            java.time.LocalTime currentTime = java.time.LocalTime.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            String formattedTime = currentTime.format(formatter);

            coolware.util.render.font.TTFFontRenderer nunito = coolware.util.render.font.AmbientFonts.getNunito(17);
            int textColor = theme != null ? theme.getSecondaryColor() : 0xFFFF4054;

            String username = (mc.thePlayer != null) ? mc.thePlayer.getName() : "Coolware";
            String userinfo = username + " \u00a77(1)";
            float width = (nunito != null ? nunito.getWidth("Ambient" + userinfo + formattedTime) : 100.0F) + 20.0F;
            float height = 18.0F;
            float rounding = 4.0F;

            // Geometry with scale applied
            float scaledW = width * scale;
            float scaledH = height * scale;
            float scaledX = startX * scale;
            float scaledY = startY * scale;

            // 1. Post-processing: Kawase Rounded Background Blur
            if (hudModule.isWatermarkBlurEnabled()) {
                IslandBlur.renderBlurRounded(scaledX, scaledY, scaledW, scaledH, 12.0F * scale, rounding * scale);
            }

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            // Scale around top-left (startX, startY)
            if (scale != 1.0F) {
                GlStateManager.translate(startX, startY, 0.0F);
                GlStateManager.scale(scale, scale, 1.0F);
                GlStateManager.translate(-startX, -startY, 0.0F);
            }

            // 2. Post-processing: Soft smooth drop shadow (no stepped outline rings)
            if (hudModule.isWatermarkShadowEnabled()) {
                RenderUtil.drawRoundedDropShadow(startX, startY + 1.5F, width, height, rounding, 4.0F, 0x55000000);
            }
            if (hudModule.isWatermarkGlowEnabled()) {
                int glowCol = (0x35 << 24) | (textColor & 0x00FFFFFF);
                RenderUtil.drawRoundedDropShadow(startX, startY, width, height, rounding, 6.0F, glowCol);
            }

            // 3. Ambient Dark Translucent Acrylic Fill (0x90121214)
            RenderUtil.drawRoundedRect(startX, startY, width, height, rounding, 0x90121214);

            // 4. Texts - Exact Ambient 3.0 margin offsets
            if (nunito != null) {
                // "Ambient" at margin 5, 5
                nunito.drawString("Ambient", startX + 5.0F, startY + 5.0F, textColor);

                // Userinfo at margin 50.0F (giving 45.5F text gap + 4.5F padding)
                nunito.drawString(userinfo, startX + 50.0F, startY + 5.0F, -1);

                // Formatted Time at 55.0F + nunito.getWidth(userinfo)
                nunito.drawString(formattedTime, startX + 55.0F + nunito.getWidth(userinfo), startY + 5.0F, -1);
            }

            // 5. Subtle vertical dividers (1px wide, 10px high, white alpha 70) matching Ambient 3.0 exact drawLine
            int dividerColor = new java.awt.Color(255, 255, 255, 70).getRGB();
            RenderUtil.drawSmoothLine(startX + 47.5F, startY + 4.0F, startX + 47.5F, startY + 14.0F, 1.0F, dividerColor);
            if (nunito != null) {
                float div2X = startX + 52.5F + nunito.getWidth(userinfo);
                RenderUtil.drawSmoothLine(div2X, startY + 4.0F, div2X, startY + 14.0F, 1.0F, dividerColor);
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }
}


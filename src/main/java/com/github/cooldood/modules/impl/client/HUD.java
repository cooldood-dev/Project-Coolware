package com.github.cooldood.modules.impl.client;

import com.github.cooldood.modules.*;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.IconFont;
import com.github.cooldood.utils.render.RenderUtil;
import com.github.cooldood.utils.render.draggable.Draggable;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.lwjgl.opengl.GL11;

@RegisterModule(
        name = "HUD",
        description = "Heads up display for client information.",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class HUD extends Module {

    public static String CLIENT_NAME = "Coolware 1.8";

    // ── Ping cache ─────────────────────────────────────────────────────────
    private static long lastPingTime = 0;
    private static int cachedPing = 0;

    private static int getPing() {
        if (C.mc.isSingleplayer()) return 0;
        long now = System.currentTimeMillis();
        if (now - lastPingTime > 2000) {
            lastPingTime = now;
            if (C.mc.getNetHandler() != null && C.p() != null) {
                net.minecraft.client.network.NetworkPlayerInfo info =
                        C.mc.getNetHandler().getPlayerInfo(C.p().getUniqueID());
                if (info != null) cachedPing = info.getResponseTime();
            }
        }
        return cachedPing;
    }

    // ── Date/Time Formats ──────────────────────────────────────────────────
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    // ── Colors ─────────────────────────────────────────────────────────────
    private static final Color PANEL_BG      = new Color(24, 24, 28, 220);    // Dark near-black translucent
    private static final Color TEXT_GRAY     = new Color(175, 175, 185, 255); // Secondary label text
    private static final Color DIVIDER_COLOR = new Color(75, 75, 90, 180);    // Subtle vertical divider

    // ── Dimensions & Spacing ───────────────────────────────────────────────
    private static final int   FONT_SIZE     = 10;
    private static final int   ICON_SIZE     = 10;
    private static final float CORNER_RAD    = 5.5f;
    private static final float PAD_X         = 8f;
    private static final float PAD_Y         = 6f;
    private static final float DIVIDER_GAP   = 6f;
    private static final float ICON_TEXT_GAP = 5f;
    private static final float ROW_GAP       = 4f;
    private static final float ACCENT_LINE_H = 2f;

    public enum WatermarkMode {
        Default,
        Dynamic_Island
    }

    @RegisterSubModule(name = "Watermark Mode")
    public static WatermarkMode watermarkMode = WatermarkMode.Default;

    public static boolean shouldHideBossBar() {
        return ModuleManager.isEnabled(HUD.class) && watermarkMode == WatermarkMode.Dynamic_Island;
    }

    // ── Watermark draggable ────────────────────────────────────────────────
    public static Draggable coolwareWatermark = new Draggable(
            "CoolWareWatermark",
            () -> {
                if (watermarkMode == WatermarkMode.Dynamic_Island) {
                    return renderDynamicIsland();
                }
                return renderDefaultWatermark();
            },
            e -> ModuleManager.isEnabled(HUD.class),
            e -> true
    );

    // ── Smooth width animation state for Dynamic Island ───────────────────────
    private static float animatedIslandWidth = 0;

    // ── Preallocated Dynamic Island visual constants ───────────────────────────
    private static final Color DI_SHADOW    = new Color(0, 0, 0, 55);
    private static final Color DI_BORDER    = new Color(255, 255, 255, 18);
    private static final Color DI_BG        = new Color(12, 12, 15, 225);
    private static final Color DI_SHEEN     = new Color(255, 255, 255, 30);
    private static final Color DI_DIVIDER   = new Color(80, 80, 95, 160);
    private static final Color DI_PING_GOOD = new Color(120, 255, 160);
    private static final Color DI_PING_BAD  = new Color(255, 100, 100);
    private static final Color DI_TIME_TXT  = new Color(155, 155, 168, 255);

    // Cache string formats to prevent string builder / GC thrash every frame
    private static int lastFpsCache = -1;
    private static String cachedFpsStr = "";
    private static int lastPingCache = -1;
    private static String cachedPingStr = "";
    private static long lastTimeCacheMs = 0;
    private static String cachedTimeStr = "";

    private static double[] renderDynamicIsland() {
        Color accent = ThemeModule.primaryColor;

        String clientName = CLIENT_NAME;
        int fps = C.mc.getDebugFPS();
        int ping = getPing();

        if (fps != lastFpsCache) {
            lastFpsCache = fps;
            cachedFpsStr = fps + " FPS";
        }
        if (ping != lastPingCache) {
            lastPingCache = ping;
            cachedPingStr = ping + "ms";
        }
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastTimeCacheMs > 1000 || cachedTimeStr.isEmpty()) {
            lastTimeCacheMs = nowMs;
            cachedTimeStr = TIME_FORMAT.format(new Date(nowMs));
        }

        String serverStr = C.mc.isSingleplayer() ? "Singleplayer" : (C.mc.getCurrentServerData() != null ? C.mc.getCurrentServerData().serverIP : "Offline");
        String pingStr   = cachedPingStr;
        String fpsStr    = cachedFpsStr;
        String timeStr   = cachedTimeStr;

        // ── Measure ────────────────────────────────────────────────────────────
        float fontH   = FontUtil.getFontHeight(FONT_SIZE);
        float divH    = fontH * 0.55f;

        float logoW    = fontH + 4f;
        float clientW  = FontUtil.getStringWidth(clientName, FONT_SIZE);
        float pingW    = FontUtil.getStringWidth(pingStr,    FONT_SIZE);
        float fpsW     = FontUtil.getStringWidth(fpsStr,     FONT_SIZE);
        float timeW    = FontUtil.getStringWidth(timeStr,    FONT_SIZE);
        float serverW  = FontUtil.getStringWidth(serverStr,  FONT_SIZE);

        float itemGap  = 12f;
        float divGap   = 9f;

        // [logo] gap [clientName] div [ping] div [fps] div [time] div [server]
        float targetContentWidth =
                logoW + itemGap
                + clientW  + divGap + 1f + divGap
                + pingW    + divGap + 1f + divGap
                + fpsW     + divGap + 1f + divGap
                + timeW    + divGap + 1f + divGap
                + serverW;

        float padX = 14f;
        float padY = 7f;
        float targetIslandWidth = padX + targetContentWidth + padX;
        float islandH   = padY + fontH + padY;
        float pillRadius = islandH / 2f;

        // ── Smooth width animation ─────────────────────────────────────────────
        if (animatedIslandWidth < 1f) animatedIslandWidth = targetIslandWidth;
        float diff = targetIslandWidth - animatedIslandWidth;
        animatedIslandWidth += diff * 0.12f;
        if (Math.abs(diff) < 0.2f) animatedIslandWidth = targetIslandWidth;
        float islandW = animatedIslandWidth;

        // ── Background: Dark Charcoal Glass Pill ──────────────────────────────
        RenderUtil.drawRoundedRect(-2f, 0f,  islandW + 4f, islandH + 3f, pillRadius + 2f, DI_SHADOW);
        RenderUtil.drawRoundedRect(-1f, -1f, islandW + 2f, islandH + 2f, pillRadius + 1f, DI_BORDER);
        RenderUtil.drawRoundedRect(0f,  0f,  islandW,      islandH,      pillRadius,      DI_BG);
        RenderUtil.drawRoundedRect(pillRadius, 0.8f, islandW - pillRadius * 2f, 0.75f, 0.4f, DI_SHEEN);

        // ── Content ───────────────────────────────────────────────────────────
        float curX    = padX;
        float contentY = padY;
        float divY    = contentY + (fontH - divH) / 2f;

        // Logo badge
        Color badgeBg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45);
        RenderUtil.drawRoundedRect(curX, contentY - 0.5f, logoW, fontH + 1f, 3.5f, badgeBg);
        String initial = clientName.substring(0, 1);
        FontUtil.drawString(initial, curX + (logoW - FontUtil.getStringWidth(initial, FONT_SIZE)) / 2f, contentY, FONT_SIZE, accent, true);
        curX += logoW + itemGap;

        // 1. Client Name
        FontUtil.drawString(clientName, curX, contentY, FONT_SIZE, accent, false);
        curX += clientW + divGap;

        RenderUtil.drawRect(curX, divY, 0.75f, divH, DI_DIVIDER);
        curX += 0.75f + divGap;

        // 2. Ping
        Color pingColor = ping < 80 ? DI_PING_GOOD : (ping < 200 ? Color.WHITE : DI_PING_BAD);
        FontUtil.drawString(pingStr, curX, contentY, FONT_SIZE, pingColor, false);
        curX += pingW + divGap;

        RenderUtil.drawRect(curX, divY, 0.75f, divH, DI_DIVIDER);
        curX += 0.75f + divGap;

        // 3. FPS
        FontUtil.drawString(fpsStr, curX, contentY, FONT_SIZE, Color.WHITE, false);
        curX += fpsW + divGap;

        RenderUtil.drawRect(curX, divY, 0.75f, divH, DI_DIVIDER);
        curX += 0.75f + divGap;

        // 4. Time
        FontUtil.drawString(timeStr, curX, contentY, FONT_SIZE, DI_TIME_TXT, false);
        curX += timeW + divGap;

        RenderUtil.drawRect(curX, divY, 0.75f, divH, DI_DIVIDER);
        curX += 0.75f + divGap;

        // 5. Server / Status
        Color serverColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210);
        FontUtil.drawString(serverStr, curX, contentY, FONT_SIZE, serverColor, false);

        return new double[]{islandW, islandH};
    }

    private static double[] renderDefaultWatermark() {
                Color accent = ThemeModule.primaryColor;

                // ── Format Strings ────────────────────────────────────────────
                String clientName = CLIENT_NAME;
                int fps = C.mc.getDebugFPS();
                int ping = getPing();

                String fpsVal = String.valueOf(fps);
                String fpsUnit = " FPS";
                String pingVal = String.valueOf(ping);
                String pingUnit = " Ping";

                Date now = new Date();
                String timeStr = TIME_FORMAT.format(now);
                String dateStr = DATE_FORMAT.format(now);

                // ── Top Panel Metrics ─────────────────────────────────────────
                float nameW = FontUtil.getStringWidth(clientName, FONT_SIZE);
                float fpsValW = FontUtil.getStringWidth(fpsVal, FONT_SIZE);
                float fpsUnitW = FontUtil.getStringWidth(fpsUnit, FONT_SIZE);
                float pingValW = FontUtil.getStringWidth(pingVal, FONT_SIZE);
                float pingUnitW = FontUtil.getStringWidth(pingUnit, FONT_SIZE);

                float topContentW = nameW
                        + (DIVIDER_GAP + 1f + DIVIDER_GAP)
                        + (fpsValW + fpsUnitW)
                        + (DIVIDER_GAP + 1f + DIVIDER_GAP)
                        + (pingValW + pingUnitW);

                float fontH = FontUtil.getFontHeight(FONT_SIZE);
                float topPanelW = PAD_X + topContentW + PAD_X;
                float topPanelH = PAD_Y + fontH + PAD_Y + ACCENT_LINE_H + 1f;

                // ── Top Panel Render ──────────────────────────────────────────
                // Dark rounded background
                RenderUtil.drawRoundedRect(0, 0, topPanelW, topPanelH, CORNER_RAD, PANEL_BG);

                // Content inside Top Panel
                float topContentY = PAD_Y;
                float curX = PAD_X;

                // 1. Client Name (Accent)
                FontUtil.drawString(clientName, curX, topContentY, FONT_SIZE, accent, false);
                curX += nameW + DIVIDER_GAP;

                // Divider 1
                float divH = fontH * 0.75f;
                float divY = topContentY + (fontH - divH) / 2f;
                RenderUtil.drawRect(curX, divY, 1f, divH, DIVIDER_COLOR);
                curX += 1f + DIVIDER_GAP;

                // 2. FPS: number in accent, unit in light gray
                FontUtil.drawString(fpsVal, curX, topContentY, FONT_SIZE, accent, false);
                curX += fpsValW;
                FontUtil.drawString(fpsUnit, curX, topContentY, FONT_SIZE, TEXT_GRAY, false);
                curX += fpsUnitW + DIVIDER_GAP;

                // Divider 2
                RenderUtil.drawRect(curX, divY, 1f, divH, DIVIDER_COLOR);
                curX += 1f + DIVIDER_GAP;

                // 3. Ping: number in accent, unit in light gray
                FontUtil.drawString(pingVal, curX, topContentY, FONT_SIZE, accent, false);
                curX += pingValW;
                FontUtil.drawString(pingUnit, curX, topContentY, FONT_SIZE, TEXT_GRAY, false);

                // Bottom Accent Line inside Top Panel
                float lineInset = 5f;
                float lineW = topPanelW - (lineInset * 2f);
                float lineY = topPanelH - ACCENT_LINE_H - 1.5f;
                RenderUtil.drawRoundedRect(lineInset, lineY, lineW, ACCENT_LINE_H, 1f, accent);

                // ── Bottom Panel Metrics ──────────────────────────────────────
                float clockIconW = IconFont.getWidth(IconFont.CLOCK, ICON_SIZE);
                float timeW = FontUtil.getStringWidth(timeStr, FONT_SIZE);
                float calIconW = IconFont.getWidth(IconFont.CALENDAR, ICON_SIZE);
                float dateW = FontUtil.getStringWidth(dateStr, FONT_SIZE);

                float bottomContentW = clockIconW + ICON_TEXT_GAP + timeW
                        + (DIVIDER_GAP + 1f + DIVIDER_GAP)
                        + calIconW + ICON_TEXT_GAP + dateW;

                float bottomPanelW = PAD_X + bottomContentW + PAD_X;
                float bottomPanelH = PAD_Y + fontH + PAD_Y;
                float bottomPanelY = topPanelH + ROW_GAP;

                // ── Bottom Panel Render ───────────────────────────────────────
                RenderUtil.drawRoundedRect(0, bottomPanelY, bottomPanelW, bottomPanelH, CORNER_RAD, PANEL_BG);

                float bContentY = bottomPanelY + PAD_Y;
                float bCurX = PAD_X;

                // Clock Icon (Accent) + Time (Gray/White)
                float iconOffsetY = bContentY + (fontH - ICON_SIZE) / 2f;
                IconFont.drawIcon(IconFont.CLOCK, bCurX, iconOffsetY, ICON_SIZE, accent);
                bCurX += clockIconW + ICON_TEXT_GAP;

                FontUtil.drawString(timeStr, bCurX, bContentY, FONT_SIZE, TEXT_GRAY, false);
                bCurX += timeW + DIVIDER_GAP;

                // Divider
                float bDivY = bContentY + (fontH - divH) / 2f;
                RenderUtil.drawRect(bCurX, bDivY, 1f, divH, DIVIDER_COLOR);
                bCurX += 1f + DIVIDER_GAP;

                // Calendar Icon (Accent) + Date (Gray/White)
                IconFont.drawIcon(IconFont.CALENDAR, bCurX, iconOffsetY, ICON_SIZE, accent);
                bCurX += calIconW + ICON_TEXT_GAP;

                FontUtil.drawString(dateStr, bCurX, bContentY, FONT_SIZE, TEXT_GRAY, false);

                // Return bounding box of entire composite HUD
                float totalHUDWidth = Math.max(topPanelW, bottomPanelW);
                float totalHUDHeight = bottomPanelY + bottomPanelH;
                return new double[]{totalHUDWidth, totalHUDHeight};
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}

    static {
        coolwareWatermark.x = 0.01;
        coolwareWatermark.y = 0.01;
    }
}

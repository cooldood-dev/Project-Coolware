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

@RegisterModule(
        name = "HUD",
        description = "Heads up display for client information.",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class HUD extends Module {

    public static String CLIENT_NAME = "Coolware";

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

    // ── Watermark draggable ────────────────────────────────────────────────
    public static Draggable coolwareWatermark = new Draggable(
            "CoolWareWatermark",
            () -> {
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
            },
            e -> ModuleManager.isEnabled(HUD.class),
            e -> true
    );

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}

    static {
        coolwareWatermark.x = 0.01;
        coolwareWatermark.y = 0.01;
    }
}

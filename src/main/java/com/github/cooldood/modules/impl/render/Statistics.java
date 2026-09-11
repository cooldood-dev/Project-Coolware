package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.ClientTickEvent;
import com.github.cooldood.events.impl.MotionEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.modules.impl.movement.Scaffold;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.TimerUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.IconFont;
import com.github.cooldood.utils.render.RenderUtil;
import com.github.cooldood.utils.render.draggable.Draggable;
import com.github.cooldood.utils.tenacity.render.RoundedUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RegisterModule(
        name = "Statistics",
        description = "Provides Statistics functionality for the client.",
        category = Category.RENDER
)
public class Statistics extends Module {
    // ── Existing data fields — DO NOT MODIFY ──────────────────────────────
    public static int gamesPlayed, killCount, deathCount, victoryCount;
    public static long startTime = System.currentTimeMillis(), endTime = -1;
    private static long lastVictoryTime = 0;
    public static final String[] KILL_TRIGGERS = {"by *", "para *", "fue destrozado a manos de *"};
    private static final Pattern WIN_MESSAGE_PATTERN = Pattern.compile(
            "(?i).*\\b(1st place!?|#1!?|victory!?|winner!?|won the game|you won(?: the game)?|team wins!?)\\b.*"
    );

    @RegisterSubModule(name = "Show Speed Graph")
    public static boolean motionGraph = true;
    @RegisterSubModule(name = "Separate Graph")
    public static boolean seprateMotionGraph = true;
    @RegisterSubModule(name = "Show BPS Counter")
    public static boolean showBps = true;
    @RegisterSubModule(name = "Separate BPS")
    public static boolean separateBps = true;
    @RegisterSubModule(name = "Size", min = 0.5, max = 2.0, increment = 0.05)
    public static double scale = 1.0;

    private static final Map<String, Double> statistics = new LinkedHashMap<>();
    private static final List<Float> speeds = new ArrayList<>();

    // ── Reference-image color palette ─────────────────────────────────────
    // Outer panel: very dark charcoal, nearly opaque
    private static final Color BG_OUTER    = new Color(22, 22, 28, 240);
    // Inner cards: slightly lighter charcoal
    private static final Color BG_INNER    = new Color(32, 32, 40, 255);
    // Icon square bg inside inner cards
    private static final Color ICON_BG     = new Color(48, 36, 64, 255);
    // Outer border: dim purple glow tint
    private static final Color BORDER_OUT  = new Color(140, 80, 220, 70);
    // Inner card border: subtle highlight
    private static final Color BORDER_IN   = new Color(255, 255, 255, 18);
    // Separator line between rows
    private static final Color SEPARATOR   = new Color(255, 255, 255, 14);
    // Primary white text
    private static final float PANEL_W     = 170f;   // compact width
    private static final float CORNER_RAD  = 3f;     // flat appearance, slight rounded
    private static final float PAD_X       = 10f;    // horiz padding
    private static final float PAD_Y       = 10f;    // vert padding
    private static final float ROW_H       = 20f;    // compact row height
    private static final float ICON_SZ     = 12f;    // small icon size
    private static final float BAR_H       = 2f;     // progress bar height

    // Font sizes
    private static final int SZ_TITLE  = 13;   // "SESSION STATS" label
    private static final int SZ_LABEL  = 11;   // stat row label
    private static final int SZ_VALUE  = 11;   // stat row value
    private static final int SZ_ICON   = 8;    // icon glyph size

    private static float width, height;

    // ── Draggables (same IDs as before — preserves saved positions) ───────
    public static Draggable dragging = new Draggable(
            "sessionstats",
            Statistics::renderStatistics,
            e -> ModuleManager.isEnabled(Statistics.class),
            e -> true
    );

    public static Draggable motionDragging = new Draggable(
            "motionGraph",
            Statistics::renderMotionGraph,
            e -> ModuleManager.isEnabled(Statistics.class) && motionGraph && seprateMotionGraph,
            e -> true
    );

    public static Draggable bpsDragging = new Draggable(
            "bpsCounter",
            Statistics::renderBpsCounter,
            e -> ModuleManager.isEnabled(Statistics.class) && showBps && separateBps,
            e -> true
    );

    // ── Accent color from theme ────────────────────────────────────────────
    private static Color accent() {
        Color[] theme = ThemeModule.getThemeColours();
        return theme[0];
    }

    private static void drawIconSquare(float x, float y, Color ac) {
        Color bg = new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 30);
        RenderUtil.drawRoundedRect(x, y, ICON_SZ, ICON_SZ, 2f, bg);
    }

    // ── Draw horizontal separator line ────────────────────────────────────
    private static void drawSep(float x, float y, float w) {
        RenderUtil.drawRect(x, y, w, 1f, new Color(255, 255, 255, 15));
    }

    // ── Main render ───────────────────────────────────────────────────────
    private static double[] renderStatistics() {
        updateSize();

        GL11.glPushMatrix();
        GL11.glScalef((float) scale, (float) scale, 1f);

        Color ac = accent();
        Color bg = new Color(22, 22, 26, 220); // Dark translucent charcoal
        Color textWhite = new Color(240, 240, 240);
        Color textGray = new Color(170, 170, 170);

        // Pre-calculate height dynamically
        float cursorY = PAD_Y;
        
        // Header
        cursorY += Math.max(ICON_SZ, FontUtil.getFontHeight(SZ_TITLE));
        cursorY += 6f; // padding below header
        cursorY += 1f; // accent line
        cursorY += 6f; // padding above stats
        
        int statCount = statistics.isEmpty() ? 4 : statistics.size();
        cursorY += statCount * ROW_H;
        
        // Play time separator
        cursorY += 6f; 
        cursorY += 1f; 
        cursorY += 6f; 
        
        // Play time row
        cursorY += Math.max(ICON_SZ, FontUtil.getFontHeight(SZ_LABEL));
        cursorY += 6f; 
        
        // Progress bar
        cursorY += BAR_H;
        cursorY += PAD_Y; 
        
        width = PANEL_W;
        height = cursorY;

        // Draw background panel (flat appearance, no shadow, tiny radius)
        RenderUtil.drawRoundedRect(0, 0, width, height, CORNER_RAD, bg);

        float curY = PAD_Y;
        
        // 1. HEADER
        float headerIconY = curY + (FontUtil.getFontHeight(SZ_TITLE) - ICON_SZ) / 2f;
        drawIconSquare(PAD_X, headerIconY, ac);
        
        // Font Awesome chart-bar icon
        float chartW = IconFont.getWidth(IconFont.CHART_BAR, SZ_ICON);
        float chartH = IconFont.getHeight(SZ_ICON);
        IconFont.drawIcon(IconFont.CHART_BAR,
                PAD_X + (ICON_SZ - chartW) / 2f,
                headerIconY + (ICON_SZ - chartH) / 2f,
                SZ_ICON, ac);
                
        float titleX = PAD_X + ICON_SZ + 6;
        float titleY = curY + (Math.max(ICON_SZ, FontUtil.getFontHeight(SZ_TITLE)) - FontUtil.getFontHeight(SZ_TITLE)) / 2f;
        
        FontUtil.drawString("SESSION ", titleX, titleY, SZ_TITLE, textWhite, false);
        float sessionW = FontUtil.getStringWidth("SESSION ", SZ_TITLE);
        FontUtil.drawString("STATS", titleX + sessionW, titleY, SZ_TITLE, ac, false);
        
        // Font Awesome chevron up icon
        float chevW = IconFont.getWidth(IconFont.CHEVRON_UP, SZ_ICON);
        float chevH = IconFont.getHeight(SZ_ICON);
        IconFont.drawIcon(IconFont.CHEVRON_UP,
                width - PAD_X - chevW,
                headerIconY + (ICON_SZ - chevH) / 2f,
                SZ_ICON, textGray);
        
        curY += Math.max(ICON_SZ, FontUtil.getFontHeight(SZ_TITLE)) + 6f;
        
        // 2. PURPLE ACCENT LINE
        RenderUtil.drawRect(0, curY, width, 1f, ac);
        curY += 1f + 6f;
        
        // 3. STATS ROWS
        for (Map.Entry<String, Double> entry : statistics.entrySet()) {
            String label = entry.getKey();
            boolean isKD = label.equals("K/D");
            String value = isKD
                    ? String.valueOf(entry.getValue().doubleValue())
                    : String.valueOf(entry.getValue().intValue());

            float rowCY = curY + ROW_H / 2f;
            float iconY = rowCY - ICON_SZ / 2f;
            
            drawIconSquare(PAD_X, iconY, ac);
            
            String iconGlyph;
            switch (label) {
                case "Games Played":
                    iconGlyph = IconFont.PLAY_CIRCLE;
                    break;
                case "Victories":
                    iconGlyph = IconFont.TROPHY_FLAG;
                    break;
                case "K/D":
                    iconGlyph = IconFont.CROSS_CLOSE;
                    break;
                case "Kills":
                    iconGlyph = IconFont.TARGET_CIRCLE;
                    break;
                default:
                    iconGlyph = IconFont.STAR;
                    break;
            }

            float rowIconW = IconFont.getWidth(iconGlyph, SZ_ICON);
            float rowIconH = IconFont.getHeight(SZ_ICON);
            IconFont.drawIcon(iconGlyph,
                    PAD_X + (ICON_SZ - rowIconW) / 2f,
                    iconY + (ICON_SZ - rowIconH) / 2f,
                    SZ_ICON, ac);
                    
            float labelY = rowCY - FontUtil.getFontHeight(SZ_LABEL) / 2f;
            FontUtil.drawString(label, PAD_X + ICON_SZ + 6, labelY, SZ_LABEL, textGray, false);
            
            Color valColor = isKD ? ac : textWhite;
            float valW = FontUtil.getStringWidth(value, SZ_VALUE);
            FontUtil.drawString(value, width - PAD_X - valW, labelY, SZ_VALUE, valColor, false);
            
            curY += ROW_H;
        }
        
        // 4. PLAY TIME SEPARATOR
        curY += 6f;
        drawSep(PAD_X, curY, width - 2*PAD_X);
        curY += 1f + 6f;
        
        // 5. PLAY TIME
        float ptCY = curY + FontUtil.getFontHeight(SZ_LABEL) / 2f;
        float ptIconY = ptCY - ICON_SZ / 2f;
        
        // Clock icon (Font Awesome 5 Free Regular)
        float clockIconW = IconFont.getWidth(IconFont.CLOCK, SZ_ICON);
        float clockIconH = IconFont.getHeight(SZ_ICON);
        float clockX = PAD_X + (ICON_SZ - clockIconW) / 2f;
        float clockY = ptCY - clockIconH / 2f;
        IconFont.drawIcon(IconFont.CLOCK, clockX, clockY, SZ_ICON, textGray);
                
        float ptLabelY = ptCY - FontUtil.getFontHeight(SZ_LABEL) / 2f;
        FontUtil.drawString("Play Time", PAD_X + ICON_SZ + 6, ptLabelY, SZ_LABEL, textGray, false);
        
        int[] playTime = getPlayTime();
        String timeStr = formatPlayTime(playTime);
        float timeW = FontUtil.getStringWidth(timeStr, SZ_VALUE);
        FontUtil.drawString(timeStr, width - PAD_X - timeW, ptLabelY, SZ_VALUE, ac, false);
        
        curY += FontUtil.getFontHeight(SZ_LABEL) + 6f;
        
        // 6. PROGRESS BAR
        float barW = width - 2*PAD_X;
        // Dark track
        RenderUtil.drawRect(PAD_X, curY, barW, BAR_H, new Color(12, 12, 16)); 
        // Purple fill based on seconds progress
        float progress = (playTime[2] % 60) / 60f; 
        RenderUtil.drawRect(PAD_X, curY, barW * progress, BAR_H, ac);

        // Note: motion graph is handled separately now

        GL11.glPopMatrix();
        return new double[]{width * scale, height * scale};
    }

    // ── Motion graph (separate draggable) ─────────────────────────────────
    private static double[] renderMotionGraph() {
        GL11.glPushMatrix();
        GL11.glScalef((float) scale, (float) scale, 1f);

        Color ac = accent();
        Color bg = new Color(22, 22, 26, 220); // Dark translucent charcoal
        Color textWhite = new Color(240, 240, 240);
        Color textGray = new Color(170, 170, 170);
        Color separatorColor = new Color(255, 255, 255, 15);

        float innerH   = FontUtil.getFontHeight(SZ_LABEL) + 8 + 50;
        float graphCardH = PAD_Y * 2 + innerH;
        float panelH     = graphCardH;

        width  = PANEL_W;
        height = panelH;

        RenderUtil.drawRoundedRect(0, 0, width, height, CORNER_RAD, bg);
        // Extremely subtle inner border
        RenderUtil.drawRoundedRectOutline(0, 0, width, height, CORNER_RAD, 0.5f, new Color(255, 255, 255, 10));

        float cursor = PAD_Y;
        FontUtil.drawString("Movement Speed", PAD_X, cursor, SZ_LABEL, textGray, false);
        String avgText = getAverageSpeed() + " bps avg";
        FontUtil.drawString(avgText,
                width - PAD_X - FontUtil.getStringWidth(avgText, SZ_LABEL),
                cursor, SZ_LABEL, ac, false);
        cursor += FontUtil.getFontHeight(SZ_LABEL) + 8;
        drawSpeedPlot(PAD_X, cursor, width - 2 * PAD_X, 50, ac);

        GL11.glPopMatrix();
        return new double[]{width * scale, height * scale};
    }

    // ── BPS Counter (separate draggable / card) ───────────────────────────
    private static double[] renderBpsCounter() {
        GL11.glPushMatrix();
        GL11.glScalef((float) scale, (float) scale, 1f);

        Color ac = accent();
        Color bg = new Color(22, 22, 26, 220); // Dark translucent charcoal
        Color textWhite = new Color(240, 240, 240);
        Color textGray = new Color(170, 170, 170);

        float cardW = 120f;
        float cardH = 34f;

        RenderUtil.drawRoundedRect(0, 0, cardW, cardH, CORNER_RAD, bg);
        RenderUtil.drawRoundedRectOutline(0, 0, cardW, cardH, CORNER_RAD, 0.5f, new Color(255, 255, 255, 10));

        float midY = cardH / 2f;
        drawIconSquare(PAD_X, midY - ICON_SZ / 2f, ac);

        float iconW = IconFont.getWidth(IconFont.COMPASS, SZ_ICON);
        float iconH = IconFont.getHeight(SZ_ICON);
        IconFont.drawIcon(IconFont.COMPASS,
                PAD_X + (ICON_SZ - iconW) / 2f,
                midY - iconH / 2f,
                SZ_ICON, ac);

        float textY = midY - FontUtil.getFontHeight(SZ_LABEL) / 2f;
        FontUtil.drawString("BPS", PAD_X + ICON_SZ + 6, textY, SZ_LABEL, textGray, false);

        String bpsVal = String.valueOf(getCurrentSpeed());
        float bpsValW = FontUtil.getStringWidth(bpsVal, SZ_VALUE);
        FontUtil.drawString(bpsVal, cardW - PAD_X - bpsValW, textY, SZ_VALUE, textWhite, false);

        GL11.glPopMatrix();
        return new double[]{cardW * scale, cardH * scale};
    }

    // ── Speed plot (unchanged logic) ──────────────────────────────────────
    private static void drawSpeedPlot(float x, float y, float w, float h, Color accent) {
        RenderUtil.drawRoundedRect(x, y, w, h, 6, new Color(15, 15, 20, 180));
        RenderUtil.drawRoundedRectOutline(x, y, w, h, 6, 0.5f, BORDER_IN);

        if (speeds.size() < 2) return;

        float plotBottom = y + h - 3;
        RenderUtil.beginRender();
        RenderUtil.beginAddingVertex(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        RenderUtil.glColor(accent);
        GL11.glLineWidth(2f);

        float length = w / (speeds.size() - 1);
        for (int i = 0; i < speeds.size(); i++) {
            float bps = speeds.get(i) * 50;
            float sx = x + i * length;
            float sy = plotBottom - Math.min(bps / 8f, 1f) * (h - 6);
            RenderUtil.addVertex(sx, sy);
        }
        RenderUtil.finishRender();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private static double getCurrentSpeed() {
        if (C.p() == null) return 0.0;
        double bps = (Math.hypot(C.p().posX - C.p().prevPosX,
                C.p().posZ - C.p().prevPosZ) * TimerUtil.getTimer()) * 20;
        return Math.round(bps * 100) / 100.0;
    }
    private static double getAverageSpeed() {
        double average = speeds.stream().collect(Collectors.averagingDouble(value -> value.doubleValue() * 50));
        return Math.round(average * 100) / 100.0;
    }

    private static String formatPlayTime(int[] playTime) {
        int h = playTime[0], m = playTime[1], s = playTime[2];
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append(":");
        if (m < 10) sb.append("0");
        sb.append(m).append(":");
        if (s < 10) sb.append("0");
        sb.append(s);
        return sb.toString();
    }

    private static void updateSize() {
        statistics.put("Games Played", (double) gamesPlayed);
        statistics.put("Victories",    (double) victoryCount);
        statistics.put("K/D",          getKillDeathRatio());
        statistics.put("Kills",        (double) killCount);
    }

    private static double getKillDeathRatio() {
        return deathCount == 0
                ? (double) killCount
                : Math.round((double) killCount / deathCount * 100) / 100.0;
    }

    // ── Existing event handlers — UNCHANGED ───────────────────────────────
    @SubscribeEvent
    public static void onChat(PacketEvent.Receive event) {
        if (C.mc.thePlayer == null) return;
        if (!(event.packet instanceof S02PacketChat)) return;

        S02PacketChat packet = (S02PacketChat) event.packet;
        String message = EnumChatFormatting.getTextWithoutFormattingCodes(
                packet.getChatComponent().getUnformattedText());
        String messageStr = packet.getChatComponent().toString();

        if (!message.contains(":") &&
                Arrays.stream(KILL_TRIGGERS).anyMatch(message.replace(C.mc.thePlayer.getName(), "*")::contains)) {
            killCount++;
        }
        String lowerMsg = message.toLowerCase();
        if (!message.contains(":") && WIN_MESSAGE_PATTERN.matcher(lowerMsg).matches()) {
            if (System.currentTimeMillis() - lastVictoryTime > 5000) {
                victoryCount++;
                lastVictoryTime = System.currentTimeMillis();
            }
        }
        if (messageStr.contains("ClickEvent{action=RUN_COMMAND, value='/play ") ||
                messageStr.contains("Want to play again?")) {
            gamesPlayed++;
        }
        if (message.contains("You died!")) {
            deathCount++;
        }
        updateSize();
    }

    @SubscribeEvent
    public static void onMotion(MotionEvent event) {
        if (speeds.size() >= 100) speeds.remove(0);
        speeds.add(getPlayerSpeed());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (endTime == -1 && (!C.mc.isSingleplayer() && C.mc.getCurrentServerData() == null)) {
            endTime = System.currentTimeMillis();
        } else if (endTime != -1 && (C.mc.isSingleplayer() || C.mc.getCurrentServerData() != null)) {
            reset();
        }
    }

    private static float getPlayerSpeed() {
        double bps = (Math.hypot(C.p().posX - C.p().prevPosX,
                C.p().posZ - C.p().prevPosZ) * TimerUtil.getTimer()) * 20;
        return (float) bps / 50;
    }

    public static int[] getPlayTime() {
        long diff = getTimeDiff();
        long diffSeconds = 0, diffMinutes = 0, diffHours = 0;
        if (diff > 0) {
            diffSeconds = diff / 1000 % 60;
            diffMinutes = diff / (60 * 1000) % 60;
            diffHours   = diff / (60 * 60 * 1000) % 24;
        }
        return new int[]{(int) diffHours, (int) diffMinutes, (int) diffSeconds};
    }

    public static long getTimeDiff() {
        return (endTime == -1 ? System.currentTimeMillis() : endTime) - startTime;
    }

    public static void reset() {
        startTime = System.currentTimeMillis();
        endTime   = -1;
        gamesPlayed = 0;
        killCount   = 0;
        deathCount  = 0;
        victoryCount = 0;
        updateSize();
    }

    @Override protected void onEnable()  { speeds.clear(); }
    @Override protected void onDisable() {}
}

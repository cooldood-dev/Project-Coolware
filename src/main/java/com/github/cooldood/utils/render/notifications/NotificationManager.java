package com.github.cooldood.utils.render.notifications;

import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.modules.impl.render.Notifications;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    private static final List<Notification> notifications = new ArrayList<>();

    // Modern Dark Theme Constants
    private static final Color BG_COLOR = new Color(13, 13, 21, 230); // Dark translucent
    private static final Color TITLE_COLOR = new Color(255, 255, 255);
    private static final Color DESC_COLOR = new Color(160, 160, 184);

    private static final int TITLE_SIZE = 12;
    private static final int DESC_SIZE = 10;
    private static final float PROGRESS_HEIGHT = 1.5f;

    public static void post(String title, String description, NotificationType type, long durationMs) {
        notifications.add(new Notification(title, description, type, durationMs));
    }

    public static void render() {
        if (notifications.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        float screenWidth = C.res().getScaledWidth();
        float screenHeight = C.res().getScaledHeight();

        // Safe Matrix & State isolation
        GL11.glPushMatrix();

        float scale = (float) Notifications.size;
        Notifications.Position pos = Notifications.position;
        boolean isRight = (pos == Notifications.Position.TopRight || pos == Notifications.Position.BottomRight);
        boolean isTop = (pos == Notifications.Position.TopRight || pos == Notifications.Position.TopLeft);

        float yOffset = isTop ? 15 : screenHeight - 15;

        // Base paddings scaled
        float leftPadding = 12f * scale;
        float rightPadding = 12f * scale;
        float padTop = 6.5f * scale;
        float padBottom = 6.5f * scale;
        float lineGap = 3f * scale;
        float effProgress = PROGRESS_HEIGHT * scale;

        int effTitleSize = (int) (TITLE_SIZE * scale);
        int effDescSize = (int) (DESC_SIZE * scale);

        float titleH = FontUtil.getFontHeight(effTitleSize);
        float descH = FontUtil.getFontHeight(effDescSize);

        // Dynamic Card Height Calculation:
        // padTop + titleH + lineGap + descH + padBottom + effProgress
        float cardHeight = padTop + titleH + lineGap + descH + padBottom + effProgress;

        // Screen edge safety
        float leftSafetyMargin = 10f;
        float rightMargin = 10f;

        // Iterate backwards for stacking top-down or bottom-up
        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification notif = notifications.get(i);

            // Dynamic Width Calculation:
            float titleWidth = FontUtil.getStringWidth(notif.getTitle(), effTitleSize);
            float descWidth = FontUtil.getStringWidth(notif.getDescription(), effDescSize);
            float contentWidth = Math.max(titleWidth, descWidth);
            float minWidth = 135f * scale;
            float cardWidth = Math.max(minWidth, contentWidth + leftPadding + rightPadding + (6f * scale));

            // Compute target coordinates based on dynamic cardWidth & cardHeight
            float targetX = isRight ? screenWidth - cardWidth - rightMargin : leftSafetyMargin;
            float targetY = isTop ? yOffset : yOffset - cardHeight;

            // Handle entry/exit
            if (notif.isExpired() || notif.getTimeLeft() < 300) {
                targetX = isRight ? screenWidth + 20 : -cardWidth - 20; // Slide off
                if (isRight ? notif.getX() >= screenWidth : notif.getX() <= -cardWidth) {
                    notifications.remove(i);
                    continue; // Skip rendering
                }
            }

            // Initialization (flag is -1, -1)
            if (notif.getX() == -1 && notif.getY() == -1) {
                notif.setX(isRight ? screenWidth + 20 : -cardWidth - 20); // Start off-screen
                notif.setY(targetY);          // At exact Y position
            }

            notif.animate(targetX, targetY);

            // Render background card and content using dynamic card dimensions
            drawModernNotification(notif.getX(), notif.getY(), cardWidth, cardHeight, padTop, lineGap, effTitleSize, effDescSize, titleH, leftPadding, rightPadding, effProgress, notif);
            
            // Advance Y for next notification
            float gap = cardHeight + (6 * scale);
            if (isTop) {
                yOffset += gap; // Stack downwards
            } else {
                yOffset -= gap; // Stack upwards
            }
        }

        GL11.glPopMatrix();
    }

    private static void drawModernNotification(float x, float y, float w, float h, float padTop, float lineGap, int effTitleSize, int effDescSize, float titleH, float leftPadding, float rightPadding, float effProgress, Notification notif) {
        GL11.glPushMatrix();

        // 1. Background rectangle covering the entire dynamic card bounds
        RenderUtil.drawRoundedRect(x, y, w, h, 2, BG_COLOR);

        // 2. Text layout positioned with exact font heights and padding
        float textY = y + padTop;
        FontUtil.drawString(notif.getTitle(), x + leftPadding, textY, effTitleSize, TITLE_COLOR, true);
        
        textY += titleH + lineGap;
        FontUtil.drawString(notif.getDescription(), x + leftPadding, textY, effDescSize, DESC_COLOR, true);

        // 3. Thin Progress Bar safely at y + cardHeight - effProgress - 1.5f
        float progress = Math.max(0, Math.min(1, (float) notif.getTimeLeft() / notif.getMaxTime()));
        if (progress > 0) {
            Color accent = RenderUtil.getColorsFade((int)(y * 10), ThemeModule.getThemeColours(), 4f);
            
            float barWidth = w - leftPadding - rightPadding;
            float barX = x + leftPadding;
            
            RenderUtil.drawRoundedRect(barX, y + h - effProgress - 1.5f, barWidth * progress, effProgress, 0, accent);
        }

        GL11.glPopMatrix();
    }
}

package com.github.cooldood.utils.render.notifications;

import com.github.cooldood.utils.client.MathUtil;
import lombok.Getter;

@Getter
public class Notification {
    private final String title;
    private final String description;
    private final NotificationType type;
    private final long maxTime;
    private final long startTime;

    private float x, y;
    private float targetX, targetY;

    public Notification(String title, String description, NotificationType type, long durationMs) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.maxTime = durationMs;
        this.startTime = System.currentTimeMillis();
        
        // Start positions for animations
        this.x = -1; // Flag as uninitialized
        this.y = -1;
    }

    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }

    public void animate(float targetX, float targetY) {
        this.targetX = targetX;
        this.targetY = targetY;

        // Smoothly interpolate current x/y to target x/y using simple lerp
        this.x = this.x + (targetX - this.x) * 0.1f;
        this.y = this.y + (targetY - this.y) * 0.1f;
    }

    public long getTimeLeft() {
        return maxTime - (System.currentTimeMillis() - startTime);
    }

    public boolean isExpired() {
        return getTimeLeft() <= 0;
    }
}

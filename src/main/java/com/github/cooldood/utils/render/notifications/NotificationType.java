package com.github.cooldood.utils.render.notifications;

import java.awt.Color;

public enum NotificationType {
    INFO(new Color(64, 156, 255)),     // Theme color fallback
    SUCCESS(new Color(64, 255, 128)),
    WARNING(new Color(255, 200, 64)),
    ERROR(new Color(255, 64, 64));

    private final Color color;

    NotificationType(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}

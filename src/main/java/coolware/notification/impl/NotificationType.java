package coolware.notification.impl;

import java.awt.Color;

public enum NotificationType {
    INFO(new Color(-1)),
    WARNING(new Color(55, 150, 0)),
    ERROR(new Color(150, 0, 0));

    private final Color color;

    NotificationType(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}

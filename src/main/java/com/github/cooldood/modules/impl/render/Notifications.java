package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RenderTickEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.render.notifications.NotificationManager;
import com.github.cooldood.utils.render.notifications.NotificationType;
import com.github.cooldood.modules.RegisterSubModule;

@RegisterModule(
        name = "Notifications",
        description = "Provides Notifications functionality for the client.",
        category = Category.RENDER
)
public class Notifications extends Module {
    public enum Position {
        TopRight, TopLeft, BottomRight, BottomLeft
    }

    @RegisterSubModule(name = "Position")
    public static Position position = Position.TopRight;

    @RegisterSubModule(name = "Size", min = 0.5, max = 2.0, increment = 0.1)
    public static double size = 1.0;

    @SubscribeEvent
    public static void onRender2D(RenderTickEvent event) {
        NotificationManager.render();
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
        // Post a test notification when disabled? Since it's disabled, it won't render anyway!
        // So we just clear or nothing.
    }
}

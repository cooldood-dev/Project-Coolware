package coolware.notification;

import coolware.notification.impl.Notification;
import coolware.notification.impl.NotificationMode;
import coolware.notification.impl.NotificationType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;

public class NotificationManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final ArrayList<Notification> notifications = new ArrayList<Notification>();
    private static String notifMode = "Modern";

    public static void update(String mode) {
        notifMode = mode;
        draw();
    }

    private static void draw() {
        if (mc == null || mc.theWorld == null) return;
        ScaledResolution sr = new ScaledResolution(mc);

        float offset = 0;
        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification notification = notifications.get(i);
            notification.draw(sr.getScaledWidth(), sr.getScaledHeight() - notification.height + offset);

            offset -= (float) ((notification.height + 5) * notification.popIn.getValue());
            if (notification.shouldDelete()) {
                notifications.remove(i);
            }
        }
    }

    public static void addNotification(String message, String subString, NotificationType type) {
        for (Notification notification : notifications) {
            if (notification.getSubString().equals(subString))
                return;
        }
        if ("Modern".equalsIgnoreCase(notifMode)) {
            notifications.add(new Notification(message, subString, type, NotificationMode.MODERN));
        }
    }
}

package coolware.notification.impl;

import coolware.util.RenderUtil;
import coolware.util.math.TimeUtil;
import coolware.util.render.animation.AmbientAnimation;
import coolware.util.render.animation.Easing;
import coolware.util.render.font.AmbientFonts;
import coolware.util.render.img.ImageManager;

import java.awt.Color;

public class Notification {

    private final String message, subString;
    private final NotificationType type;
    private final NotificationMode mode;
    public float width;
    public float height;
    public final AmbientAnimation popIn = new AmbientAnimation(Easing.EASE_IN_OUT_SINE, 250);
    private final TimeUtil time = new TimeUtil();

    public Notification(String message, String subString, NotificationType type, NotificationMode mode) {
        this.mode = mode;
        this.message = message;
        this.subString = subString;
        this.type = type;
        setSize(mode);
        time.reset();
    }

    private void setSize(NotificationMode mode) {
        switch (mode) {
            case MODERN:
                this.width = 125;
                this.height = 32;
                break;
        }
    }

    public final void draw(final float x, final float y) {
        popIn.run(time.getTime() < 250 || time.getTime() < 1725 ? 1 : 0);

        switch (mode) {
            case MODERN:
                float modernTitleWidth = AmbientFonts.getNunito(19).getWidth(message);
                float modernSubWidth = AmbientFonts.getNunito(16).getWidth(subString);
                this.width = modernTitleWidth > modernSubWidth ? modernTitleWidth + 50 : modernSubWidth + 50;

                float newXModern = (float) (x - width * popIn.getValue() - 5);
                float newYModern = (float) (y - 25 * popIn.getValue());

                RenderUtil.drawRoundedRect(newXModern, newYModern, width, height, 7, new Color(0x90121214, true));
                RenderUtil.drawRoundedRect(newXModern + 3, newYModern + 3, height - 6, height - 6, 4, new Color(0x65000000, true));
                if (ImageManager.inGameImages.containsKey("info")) {
                    ImageManager.inGameImages.get("info").drawImg(newXModern + 6, newYModern + 6, height - 12, height - 12, new Color(150, 150, 150));
                }
                AmbientFonts.getNunito(19).drawStringWithShadow(message, newXModern + 32, newYModern + 5, 0xFFFFFFFF);
                AmbientFonts.getNunito(16).drawString(subString, newXModern + 32, newYModern + 18, new Color(150, 150, 150).getRGB());
                break;
        }
    }

    public boolean shouldDelete() {
        return time.finished(2000);
    }

    public String getMessage() {
        return message;
    }

    public String getSubString() {
        return subString;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationMode getMode() {
        return mode;
    }
}

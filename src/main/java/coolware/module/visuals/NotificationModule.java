package coolware.module.visuals;

import coolware.module.Category;
import coolware.module.Module;
import coolware.notification.NotificationManager;
import coolware.setting.ModeSetting;
import org.lwjgl.input.Keyboard;

public class NotificationModule extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Modern", "Modern");

    public NotificationModule() {
        super("Notification", "Displays pop-up alerts for specific events or actions.", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(mode);
        setToggled(true);
    }

    @Override
    public void onRender2D() {
        NotificationManager.update(mode.getMode());
    }

    public ModeSetting getMode() {
        return mode;
    }
}

package coolware.module.movement;

import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import org.lwjgl.input.Keyboard;

public class Fly extends Module {
    public static ModeSetting mode;
    public static NumberSetting speed;

    public Fly() {
        super("Fly", "Allows free flight in survival mode", Category.MOVEMENT, Keyboard.KEY_F);
        mode = new ModeSetting("Mode", this, "Vanilla", "Vanilla", "Motion", "Glide");
        speed = new NumberSetting("Speed", this, 1.0, 0.1, 5.0, 0.1);
        addSetting(mode);
        addSetting(speed);
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer != null) {
            mc.thePlayer.capabilities.isFlying = true;
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null && !mc.thePlayer.capabilities.isCreativeMode) {
            mc.thePlayer.capabilities.isFlying = false;
        }
    }
}

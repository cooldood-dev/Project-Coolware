package coolware.module.combat;

import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.NumberSetting;
import org.lwjgl.input.Keyboard;

public class Reach extends Module {
    public static NumberSetting distance;

    public Reach() {
        super("Reach", "Increases attack and interact distance", Category.COMBAT, Keyboard.KEY_NONE);
        distance = new NumberSetting("Distance", this, 3.4, 3.0, 6.0, 0.1);
        addSetting(distance);
    }
}

package coolware.module.visuals;

import coolware.module.Category;
import coolware.module.Module;
import org.lwjgl.input.Keyboard;

public class Fullbright extends Module {
    private float prevGamma;

    public Fullbright() {
        super("Fullbright", "Brightens the world for perfect night vision", Category.VISUALS, Keyboard.KEY_B);
    }

    @Override
    public void onEnable() {
        if (mc.gameSettings != null) {
            prevGamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 1000.0F;
        }
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            mc.gameSettings.gammaSetting = prevGamma;
        }
    }
}

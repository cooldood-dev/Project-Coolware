package coolware.module.visuals;

import coolware.config.AnimationConfig;
import coolware.config.AnimationMode;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import org.lwjgl.input.Keyboard;

public class Animations extends Module {

    private static final String[] MODES = new String[]{
            "VANILLA", "EXHIBITION", "ETB", "SIGMA", "DORTWARE", "PLAIN",
            "SPIN", "AVATAR", "SWONG", "SWANG", "SWANK", "STYLES",
            "NUDGE", "PUNCH", "JIGSAW", "SLIDE",
            "Swing", "Old", "Push", "Dash", "Slash", "Scale", "Swonk", "Stella",
            "Small", "Edit", "Rhys", "Stab", "Float", "Remix", "Xiv", "Winter",
            "Yamato", "SlideSwing", "SmallPush", "Reverse", "Invent", "Leaked",
            "Aqua", "Astro", "Fadeaway", "Astolfo", "AstolfoSpin", "Moon",
            "MoonPush", "Smooth", "Tap1", "Tap2", "Sigma3", "Sigma4",
            "1.8"
    };

    public final ModeSetting mode = new ModeSetting("Mode", this, "VANILLA", MODES);
    public final ModeSetting render = new ModeSetting("Render", this, "ALWAYS", "BLOCKING", "ALWAYS");

    public final NumberSetting scale = new NumberSetting("Scale", this, 100.0, 50.0, 150.0, 1.0);
    public final NumberSetting itemSize = new NumberSetting("Item Size", this, 0.0, -0.5, 0.5, 0.05);
    public final NumberSetting blockPosX = new NumberSetting("BlockPos X", this, 0.0, -1.0, 1.0, 0.05);
    public final NumberSetting blockPosY = new NumberSetting("BlockPos Y", this, 0.0, -1.0, 1.0, 0.05);
    public final NumberSetting blockPosZ = new NumberSetting("BlockPos Z", this, 0.0, -1.0, 1.0, 0.05);
    public final NumberSetting swingSpeed = new NumberSetting("SwingSpeed", this, 0.0, 0.0, 100.0, 1.0);

    public Animations() {
        super("Animations", "Customizes player animations like swinging and blocking", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(mode);
        addSetting(render);
        addSetting(scale);
        addSetting(itemSize);
        addSetting(blockPosX);
        addSetting(blockPosY);
        addSetting(blockPosZ);
        addSetting(swingSpeed);
    }

    @Override
    public void onEnable() {
        syncConfig();
    }

    @Override
    public void onDisable() {
        AnimationConfig.setEnabled(false);
    }

    public void syncConfig() {
        AnimationConfig.setEnabled(isToggled());
        AnimationConfig.setMode(AnimationMode.fromString(mode.getMode()));
        AnimationConfig.setRenderMode("ALWAYS".equalsIgnoreCase(render.getMode()) ? 1 : 0);
        AnimationConfig.setScale(scale.getValueInt());
        AnimationConfig.setItemSize(itemSize.getValueFloat());
        AnimationConfig.setBlockPosX(blockPosX.getValueFloat());
        AnimationConfig.setBlockPosY(blockPosY.getValueFloat());
        AnimationConfig.setBlockPosZ(blockPosZ.getValueFloat());
        AnimationConfig.setSwingSpeed(swingSpeed.getValueInt());
    }

    @Override
    public void onUpdate() {
        if (isToggled()) {
            syncConfig();
        }
    }
}

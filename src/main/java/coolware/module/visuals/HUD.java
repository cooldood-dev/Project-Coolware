package coolware.module.visuals;

import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.BooleanSetting;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import org.lwjgl.input.Keyboard;

public class HUD extends Module {
    // Watermark Settings
    private final ModeSetting watermarkMode = new ModeSetting("Watermark Mode", this, "Dynamic Island", "Dynamic Island", "Simple", "Nursultan", "Ambient", "None");
    private final NumberSetting watermarkScale = new NumberSetting("Watermark Scale", this, 1.0, 0.5, 2.5, 0.05);
    private final BooleanSetting watermarkBlur = new BooleanSetting("Watermark Blur", this, true);
    private final BooleanSetting watermarkShadow = new BooleanSetting("Watermark Shadow", this, true);
    private final BooleanSetting watermarkAnimation = new BooleanSetting("Watermark Animation", this, true);
    private final NumberSetting watermarkAnimationSpeed = new NumberSetting("Watermark Anim Speed", this, 8.0, 2.0, 20.0, 0.5);
    private final BooleanSetting watermarkGlow = new BooleanSetting("Watermark Glow", this, true);
    private final BooleanSetting watermarkLogo = new BooleanSetting("Watermark Logo", this, true);
    private final BooleanSetting watermarkVersion = new BooleanSetting("Watermark Version", this, true);
    private final BooleanSetting watermarkFps = new BooleanSetting("Watermark FPS", this, true);
    private final NumberSetting watermarkX = new NumberSetting("Watermark X", this, 8.0, 0.0, 1000.0, 1.0);
    private final NumberSetting watermarkY = new NumberSetting("Watermark Y", this, 8.0, 0.0, 1000.0, 1.0);

    private final NumberSetting fontSizeSetting = new NumberSetting("Font Size", this, 18.0, 12.0, 30.0, 1.0);

    public HUD() {
        super("HUD", "Displays client watermark and information", Category.VISUALS, Keyboard.KEY_NONE);
        // Register Watermark settings
        addSetting(watermarkMode);
        addSetting(watermarkScale);
        addSetting(watermarkBlur);
        addSetting(watermarkShadow);
        addSetting(watermarkAnimation);
        addSetting(watermarkAnimationSpeed);
        addSetting(watermarkGlow);
        addSetting(watermarkLogo);
        addSetting(watermarkVersion);
        addSetting(watermarkFps);
        addSetting(watermarkX);
        addSetting(watermarkY);
        addSetting(fontSizeSetting);
        setToggled(true);
    }

    public ModeSetting getWatermarkMode() {
        return watermarkMode;
    }

    public float getWatermarkScale() {
        return watermarkScale.getValueFloat();
    }

    public boolean isWatermarkBlurEnabled() {
        return watermarkBlur.isEnabled();
    }

    public boolean isWatermarkShadowEnabled() {
        return watermarkShadow.isEnabled();
    }

    public boolean isWatermarkAnimationEnabled() {
        return watermarkAnimation.isEnabled();
    }

    public float getWatermarkAnimationSpeed() {
        return watermarkAnimationSpeed.getValueFloat();
    }

    public boolean isWatermarkGlowEnabled() {
        return watermarkGlow.isEnabled();
    }

    public boolean isWatermarkLogoEnabled() {
        return watermarkLogo.isEnabled();
    }

    public boolean isWatermarkVersionEnabled() {
        return watermarkVersion.isEnabled();
    }

    public boolean isWatermarkFpsEnabled() {
        return watermarkFps.isEnabled();
    }

    public float getWatermarkX() {
        return watermarkX.getValueFloat();
    }

    public float getWatermarkY() {
        return watermarkY.getValueFloat();
    }

    public int getFontSize() {
        return fontSizeSetting.getValueInt();
    }
}

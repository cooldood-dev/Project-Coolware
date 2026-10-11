package coolware.module.visuals;

import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.ModeSetting;
import org.lwjgl.input.Keyboard;

public class Freelook extends Module {
    public static float cameraYaw;
    public static float cameraPitch;
    public static float prevCameraYaw;
    public static float prevCameraPitch;

    private int previousPerspective = 0;
    private final ModeSetting mode = new ModeSetting("Mode", this, "Hold", "Hold", "Toggle");

    public Freelook() {
        super("Freelook", "Allows looking around without changing movement direction", Category.VISUALS, Keyboard.KEY_LMENU);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            cameraYaw = prevCameraYaw = mc.thePlayer.rotationYaw;
            cameraPitch = prevCameraPitch = mc.thePlayer.rotationPitch;
            if (mc.gameSettings != null) {
                previousPerspective = mc.gameSettings.thirdPersonView;
                mc.gameSettings.thirdPersonView = 1;
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            cameraYaw = prevCameraYaw = mc.thePlayer.rotationYaw;
            cameraPitch = prevCameraPitch = mc.thePlayer.rotationPitch;
            if (mc.gameSettings != null) {
                mc.gameSettings.thirdPersonView = previousPerspective;
            }
        }
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        // In Hold mode, disable freelook as soon as key is released
        if (mode.is("Hold") && getKeyCode() != Keyboard.KEY_NONE) {
            if (!Keyboard.isKeyDown(getKeyCode())) {
                setToggled(false);
            }
        }
    }

    public static void updateAngles(float deltaYaw, float deltaPitch) {
        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;

        cameraYaw += deltaYaw * 0.15F;
        cameraPitch -= deltaPitch * 0.15F;

        if (cameraPitch > 90.0F) {
            cameraPitch = 90.0F;
        }
        if (cameraPitch < -90.0F) {
            cameraPitch = -90.0F;
        }
    }
}

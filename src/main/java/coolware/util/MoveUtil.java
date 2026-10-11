package coolware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;

public class MoveUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isForwardPressed() {
        if (mc.gameSettings == null) return false;
        if (mc.gameSettings.keyBindForward.isKeyDown() != mc.gameSettings.keyBindBack.isKeyDown())
            return true;
        return mc.gameSettings.keyBindLeft.isKeyDown() != mc.gameSettings.keyBindRight.isKeyDown();
    }

    public static int getForwardValue() {
        if (mc.gameSettings == null) return 0;
        int forwardValue = 0;
        if (mc.gameSettings.keyBindForward.isKeyDown()) {
            ++forwardValue;
        }
        if (mc.gameSettings.keyBindBack.isKeyDown()) {
            --forwardValue;
        }
        return forwardValue;
    }

    public static int getLeftValue() {
        if (mc.gameSettings == null) return 0;
        int leftValue = 0;
        if (mc.gameSettings.keyBindLeft.isKeyDown()) {
            ++leftValue;
        }
        if (mc.gameSettings.keyBindRight.isKeyDown()) {
            --leftValue;
        }
        return leftValue;
    }

    public static float adjustYaw(float yaw, float forward, float strafe) {
        if (forward < 0.0f) {
            yaw += 180.0f;
        }
        if (strafe != 0.0f) {
            float multiplier = forward == 0.0f ? 1.0f : 0.5f * Math.signum(forward);
            yaw += -90.0f * multiplier * Math.signum(strafe);
        }
        return MathHelper.wrapAngleTo180_float(yaw);
    }

    public static void fixStrafe(float targetYaw) {
        if (mc.thePlayer == null || mc.thePlayer.movementInput == null) return;
        fixStrafe(targetYaw, mc.thePlayer.movementInput);
    }

    public static void fixStrafe(float targetYaw, MovementInput movementInput) {
        if (mc.thePlayer == null || movementInput == null) return;
        float angle = MathHelper.wrapAngleTo180_float(adjustYaw(mc.thePlayer.rotationYaw, getForwardValue(), getLeftValue()) - targetYaw + 22.5f);
        switch ((int) (angle + 180.0f) / 45 % 8) {
            case 0: {
                movementInput.moveForward = -1.0f;
                movementInput.moveStrafe = 0.0f;
                break;
            }
            case 1: {
                movementInput.moveForward = -1.0f;
                movementInput.moveStrafe = 1.0f;
                break;
            }
            case 2: {
                movementInput.moveForward = 0.0f;
                movementInput.moveStrafe = 1.0f;
                break;
            }
            case 3: {
                movementInput.moveForward = 1.0f;
                movementInput.moveStrafe = 1.0f;
                break;
            }
            case 4: {
                movementInput.moveForward = 1.0f;
                movementInput.moveStrafe = 0.0f;
                break;
            }
            case 5: {
                movementInput.moveForward = 1.0f;
                movementInput.moveStrafe = -1.0f;
                break;
            }
            case 6: {
                movementInput.moveForward = 0.0f;
                movementInput.moveStrafe = -1.0f;
                break;
            }
            case 7: {
                movementInput.moveForward = -1.0f;
                movementInput.moveStrafe = -1.0f;
                break;
            }
        }
        if (movementInput.sneak) {
            movementInput.moveForward *= 0.3f;
            movementInput.moveStrafe *= 0.3f;
        }
    }
}

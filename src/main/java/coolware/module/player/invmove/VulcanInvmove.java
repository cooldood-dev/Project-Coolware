package coolware.module.player.invmove;

import coolware.module.Module;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public class VulcanInvmove extends ModuleMode {
    private double maxSpeed = 0;

    public VulcanInvmove(String modeName, Module module) {
        super(modeName, module);
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.gameSettings == null) return;
        updateKeyBindings();

        if (mc.currentScreen instanceof GuiInventory) {
            if (maxSpeed == 0) maxSpeed = MathHelper.clamp_double(speed(), 0, 0.155);
            strafe(maxSpeed * 0.999);
            maxSpeed *= 0.999;
        } else {
            if (maxSpeed != 0) {
                maxSpeed = 0;
            }
        }
    }

    private double speed() {
        if (mc.thePlayer == null) return 0.0;
        return Math.hypot(Math.abs(mc.thePlayer.motionX), Math.abs(mc.thePlayer.motionZ));
    }

    private boolean moving() {
        if (mc.thePlayer == null || mc.thePlayer.movementInput == null) return false;
        return mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0;
    }

    private double getDirection() {
        if (mc.thePlayer == null) return 0.0;
        float moveForward = mc.thePlayer.moveForward;
        float moveStrafing = mc.thePlayer.moveStrafing;
        float rotationYaw = mc.thePlayer.rotationYaw;

        boolean isGoingBackward = moveForward < 0F;
        boolean isGoingForward = moveForward > 0F;
        float forward = 1F;
        if (isGoingBackward) {
            rotationYaw += 180F;
        }
        if (isGoingBackward) forward = -0.5F;
        else if (isGoingForward) forward = 0.5F;
        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;
        return Math.toRadians(rotationYaw);
    }

    private void strafe(final double speed) {
        if (!moving() || mc.thePlayer == null)
            return;

        double yaw = getDirection();

        mc.thePlayer.motionX = -MathHelper.sin((float) yaw) * speed;
        mc.thePlayer.motionZ = MathHelper.cos((float) yaw) * speed;
    }

    private void updateKeyBindings() {
        if (mc.gameSettings == null) return;
        mc.gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        mc.gameSettings.keyBindBack.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        mc.gameSettings.keyBindLeft.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
        mc.gameSettings.keyBindRight.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
        mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
    }
}

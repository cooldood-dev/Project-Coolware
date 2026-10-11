package coolware.module.movement;

import coolware.module.Category;
import coolware.module.Module;
import coolware.util.MovementFixUtil;
import org.lwjgl.input.Keyboard;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Automatically sprints when walking forward", Category.MOVEMENT, Keyboard.KEY_V);
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null) return;

        // Do not force sprint if Scaffold or silent rotations explicitly block sprint
        if (MovementFixUtil.sprintBlocked()) return;

        // When move-fix remaps keys to silent look, the remapped forward input is in movementInput.moveForward.
        // In normal conditions, mc.thePlayer.moveForward holds the forward speed.
        // Sprint is allowed whenever moving forward and not sneaking or colliding.
        float forward = MovementFixUtil.remaps() && mc.thePlayer.movementInput != null
                ? mc.thePlayer.movementInput.moveForward
                : mc.thePlayer.moveForward;

        if (forward > 0.0F && !mc.thePlayer.isSneaking() && !mc.thePlayer.isCollidedHorizontally) {
            mc.thePlayer.setSprinting(true);
        }
    }
}

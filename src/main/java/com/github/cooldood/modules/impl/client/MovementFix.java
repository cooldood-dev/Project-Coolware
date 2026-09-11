package com.github.cooldood.modules.impl.client;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MovementInputEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.modules.impl.movement.Scaffold;
import com.github.cooldood.modules.impl.render.Freecam;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.MathUtil;
import com.github.cooldood.utils.minecraft.PlayerUtil;
import lombok.AllArgsConstructor;
import net.minecraft.entity.Entity;

@RegisterModule(
        name = "Movement Fix",
        description = "Provides Movement Fix functionality for the client.",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class MovementFix extends Module {
    @RegisterSubModule(name = "Fix Movement", description = "Fixes movement client side to be accurate to the server side rotation")
    public static boolean movementFix = true;
    @RegisterSubModule(name = "Move Fix Mode", parent = "Fix Movement")
    public static MoveFixMode moveFixMode = MoveFixMode.Straight;
    public enum MoveFixMode {
        Vanilla,
        Smooth,
        Straight
    }

    @RegisterSubModule(name = "Fix Rotation", description = "Makes sure your hovering the correct block/entity that you are server side")
    public static boolean rotationFix = true;

    private static float yawDeficit = 0;

    public static boolean shouldMoveFix(Entity instance) {
        return movementFix
                && instance == C.p()
                && ModuleManager.isEnabled(MovementFix.class)
                && !ModuleManager.isEnabled(Freecam.class);
    }
    public static boolean shouldRotationFix() {
        return ((rotationFix && ModuleManager.isEnabled(MovementFix.class))
                || (Scaffold.isShouldScaffold() && Scaffold.manualPlace))
                && !ModuleManager.isEnabled(Freecam.class);
    }

    // goes first to allow the inputs to be modified later
    @SubscribeEvent(priority = 1)
    public static void onMovementInputEvent(MovementInputEvent event) {
        if (!shouldMoveFix(C.p())) return;
        if (moveFixMode == MoveFixMode.Vanilla) return;

        float speed = Math.max(Math.abs(event.movementInput.moveForward), Math.abs(event.movementInput.moveStrafe));
        // make sure ur moving
        if (speed == 0) return;

        MovementDirection fixedMovementDirection = getMovementDirection(event);

        // fix movement direction to one closest to rotation
        event.movementInput.moveForward = fixedMovementDirection.forward ? speed : (fixedMovementDirection.back ? -speed : 0);
        event.movementInput.moveStrafe = fixedMovementDirection.left ? speed : (fixedMovementDirection.right ? -speed : 0);
    }

    private static MovementDirection getMovementDirection(MovementInputEvent event) {
        float yawDifference = C.p().rotationYaw - PlayerUtil.currentRotation().yaw;

        if (moveFixMode == MoveFixMode.Straight) {
            float yawDeficitAdded = MathUtil.toNearest(yawDifference + yawDeficit, 45) - yawDifference;

            yawDifference += yawDeficitAdded;
            yawDeficit -= yawDeficitAdded;

            // calculate how much yaw precision we have lost.
            yawDeficit += MathUtil.toNearest(yawDifference, 45) - yawDifference;
        }
        else yawDifference += yawDifference < 0 ? -22.5f : 22.5f;

        int yawOrdinal = Math.floorMod((int) (yawDifference / 45), MovementDirection.values().length);

        // get move direction
        MovementDirection inputMoveDirection = event.movementInput.moveStrafe < 0 ? MovementDirection.EAST : MovementDirection.WEST;
        if (event.movementInput.moveForward > 0) inputMoveDirection = event.movementInput.moveStrafe < 0 ? MovementDirection.NORTH_EAST : (event.movementInput.moveStrafe > 0 ? MovementDirection.NORTH_WEST : MovementDirection.NORTH);
        if (event.movementInput.moveForward < 0) inputMoveDirection = event.movementInput.moveStrafe < 0 ? MovementDirection.SOUTH_EAST : (event.movementInput.moveStrafe > 0 ? MovementDirection.SOUTH_WEST : MovementDirection.SOUTH);

        return MovementDirection.values()[Math.floorMod(inputMoveDirection.ordinal() + yawOrdinal, MovementDirection.values().length)];
    }

    @AllArgsConstructor
    private enum MovementDirection {
        NORTH       (   true,    false,  false,  false  ),
        NORTH_EAST  (   true,    true,   false,  false  ),
        EAST        (   false,   true,   false,  false  ),
        SOUTH_EAST  (   false,   true,   true,   false  ),
        SOUTH       (   false,   false,  true,   false  ),
        SOUTH_WEST  (   false,   false,  true,   true   ),
        WEST        (   false,   false,  false,  true   ),
        NORTH_WEST  (   true,    false,  false,  true   );

        private final boolean forward, right, back, left;
    }

    @Override
    protected void onEnable() {
        yawDeficit = 0;
    }

    @Override
    protected void onDisable() {

    }
}

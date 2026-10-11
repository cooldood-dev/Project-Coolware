package coolware.util;

import coolware.module.combat.KillAura;
import coolware.module.movement.Scaffold;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;

public class MovementFixUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static float yawDeficit = 0.0F;

    public static boolean shouldMoveFix() {
        if (mc.thePlayer == null || !RotationUtil.silentActive) return false;
        if (coolware.module.player.BedBreaker.wantsMovementFix()) return true;
        if (KillAura.isActivelyAiming()) return true;
        return Scaffold.wantsMoveFix();
    }

    // Scaffold / KillAura / BedBreaker walks the keys relative to its look
    private static String mode() {
        if (coolware.module.player.BedBreaker.wantsMovementFix()) {
            return coolware.module.player.BedBreaker.getMoveFixMode();
        }
        if (KillAura.isActivelyAiming() && KillAura.moveFix != null) {
            String kaFix = KillAura.moveFix.getMode();
            if ("NONE".equalsIgnoreCase(kaFix)) {
                return "Vanilla";
            } else if ("Silent".equalsIgnoreCase(kaFix)) {
                return "Smooth";
            } else if ("Strict".equalsIgnoreCase(kaFix)) {
                return "Straight";
            }
        }
        return "Straight";
    }

    /** The keys are being remapped to the silent look this tick. */
    public static boolean remaps() {
        return shouldMoveFix() && !"Vanilla".equalsIgnoreCase(mode());
    }

    /**
     * The yaw the player's movement is worked out with (Entity.moveFlying,
     * EntityLivingBase.jump's sprint boost). While the keys are remapped to the
     * silent look, the movement has to use that look too -- the server
     * simulates it with the yaw it was sent -- or the remapped keys walk the
     * wrong way: W with the look behind you became S, walking backwards.
     */
    public static float movementYaw(Entity entity, float ownYaw) {
        if (entity == null || entity != mc.thePlayer || !remaps()) return ownYaw;
        return RotationUtil.serverYaw;
    }

    /**
     * Sprinting is off while Scaffold holds it (Sprint off), and while a silent
     * look is sent that the keys aren't remapped to: the sprint would point
     * away from the look the server has. With the remap, vanilla's own rule
     * (sprint only while moving forward) already keeps it within the look.
     *
     * Note: the external Sprint module is guarded separately in Sprint.onUpdate()
     * by checking shouldMoveFix(), so it never races move-fix via this path.
     */
    public static boolean sprintBlocked() {
        return Scaffold.blocksSprint() || (RotationUtil.silentActive && !remaps());
    }

    public static void fixMovement(MovementInput movementInput) {
        if (!remaps()) {
            yawDeficit = 0.0F;
            return;
        }

        // When BedBreaker is actively aiming, apply ScaleHack's movefix exclusively for BedBreaker!
        if (coolware.module.player.BedBreaker.wantsMovementFix()) {
            applyScaleHackMoveFix(movementInput, coolware.module.player.BedBreaker.getMoveFixMode());
            return;
        }

        // If Scaffold is active, keep Scaffold's existing movefix logic 100% untouched
        if (Scaffold.wantsMoveFix()) {
            String mode = mode();
            float speed = Math.max(Math.abs(movementInput.moveForward), Math.abs(movementInput.moveStrafe));
            if (speed == 0.0F) return;

            MovementDirection fixedMovementDirection = getMovementDirection(movementInput, mode);

            movementInput.moveForward = fixedMovementDirection.forward ? speed : (fixedMovementDirection.back ? -speed : 0.0F);
            movementInput.moveStrafe = fixedMovementDirection.left ? speed : (fixedMovementDirection.right ? -speed : 0.0F);
            return;
        }

        // For KillAura: Use OpenMyau-Plus fixStrafe 1:1
        if (KillAura.isActivelyAiming()) {
            fixStrafeOpenMyau(movementInput, RotationUtil.serverYaw);
        }
    }

    /**
     * ScaleHack's MovementFix algorithm directly ported and isolated for BedBreaker only
     */
    private static void applyScaleHackMoveFix(MovementInput movementInput, String mode) {
        if ("Vanilla".equalsIgnoreCase(mode)) return;

        float speed = Math.max(Math.abs(movementInput.moveForward), Math.abs(movementInput.moveStrafe));
        if (speed == 0.0F) return;

        float yawDifference = mc.thePlayer.rotationYaw - RotationUtil.serverYaw;

        if ("Straight".equalsIgnoreCase(mode)) {
            float yawDeficitAdded = MathUtil.toNearest(yawDifference + yawDeficit, 45.0F) - yawDifference;
            yawDifference += yawDeficitAdded;
            yawDeficit -= yawDeficitAdded;
            yawDeficit += MathUtil.toNearest(yawDifference, 45.0F) - yawDifference;
        } else {
            // Smooth mode in ScaleHack
            yawDifference += yawDifference < 0.0F ? -22.5F : 22.5F;
        }

        int yawOrdinal = Math.floorMod((int) (yawDifference / 45.0F), MovementDirection.values().length);

        MovementDirection inputMoveDirection = movementInput.moveStrafe < 0.0F ? MovementDirection.EAST : MovementDirection.WEST;
        if (movementInput.moveForward > 0.0F) {
            inputMoveDirection = movementInput.moveStrafe < 0.0F ? MovementDirection.NORTH_EAST : (movementInput.moveStrafe > 0.0F ? MovementDirection.NORTH_WEST : MovementDirection.NORTH);
        } else if (movementInput.moveForward < 0.0F) {
            inputMoveDirection = movementInput.moveStrafe < 0.0F ? MovementDirection.SOUTH_EAST : (movementInput.moveStrafe > 0.0F ? MovementDirection.SOUTH_WEST : MovementDirection.SOUTH);
        }

        MovementDirection fixedMovementDirection = MovementDirection.values()[Math.floorMod(inputMoveDirection.ordinal() + yawOrdinal, MovementDirection.values().length)];

        movementInput.moveForward = fixedMovementDirection.forward ? speed : (fixedMovementDirection.back ? -speed : 0.0F);
        movementInput.moveStrafe = fixedMovementDirection.left ? speed : (fixedMovementDirection.right ? -speed : 0.0F);
    }

    /**
     * Ported 1:1 from OpenMyau-Plus MoveUtil.fixStrafe(targetYaw) and MoveUtil.adjustYaw(...)
     */
    public static void fixStrafeOpenMyau(MovementInput movementInput, float targetYaw) {
        int forwardValue = 0;
        if (mc.gameSettings.keyBindForward.isKeyDown()) forwardValue++;
        if (mc.gameSettings.keyBindBack.isKeyDown())    forwardValue--;

        int leftValue = 0;
        if (mc.gameSettings.keyBindLeft.isKeyDown())  leftValue++;
        if (mc.gameSettings.keyBindRight.isKeyDown()) leftValue--;

        if (forwardValue == 0 && leftValue == 0) {
            return;
        }

        float playerYaw = mc.thePlayer.rotationYaw;
        if (forwardValue < 0) {
            playerYaw += 180.0F;
        }
        if (leftValue != 0) {
            float multiplier = forwardValue == 0 ? 1.0F : 0.5F * Math.signum((float) forwardValue);
            playerYaw += -90.0F * multiplier * Math.signum((float) leftValue);
        }
        playerYaw = MathHelper.wrapAngleTo180_float(playerYaw);

        float angle = MathHelper.wrapAngleTo180_float(playerYaw - targetYaw + 22.5F);
        switch ((int) (angle + 180.0F) / 45 % 8) {
            case 0:
                movementInput.moveForward = -1.0F;
                movementInput.moveStrafe = 0.0F;
                break;
            case 1:
                movementInput.moveForward = -1.0F;
                movementInput.moveStrafe = 1.0F;
                break;
            case 2:
                movementInput.moveForward = 0.0F;
                movementInput.moveStrafe = 1.0F;
                break;
            case 3:
                movementInput.moveForward = 1.0F;
                movementInput.moveStrafe = 1.0F;
                break;
            case 4:
                movementInput.moveForward = 1.0F;
                movementInput.moveStrafe = 0.0F;
                break;
            case 5:
                movementInput.moveForward = 1.0F;
                movementInput.moveStrafe = -1.0F;
                break;
            case 6:
                movementInput.moveForward = 0.0F;
                movementInput.moveStrafe = -1.0F;
                break;
            case 7:
                movementInput.moveForward = -1.0F;
                movementInput.moveStrafe = -1.0F;
                break;
        }

        if (movementInput.sneak) {
            movementInput.moveForward *= 0.3F;
            movementInput.moveStrafe *= 0.3F;
        }
    }

    private static MovementDirection getMovementDirection(MovementInput movementInput, String mode) {
        float serverYaw = RotationUtil.serverYaw;
        float yawDifference = mc.thePlayer.rotationYaw - serverYaw;

        if ("Straight".equalsIgnoreCase(mode)) {
            float yawDeficitAdded = MathUtil.toNearest(yawDifference + yawDeficit, 45.0F) - yawDifference;
            yawDifference += yawDeficitAdded;
            yawDeficit -= yawDeficitAdded;
            yawDeficit += MathUtil.toNearest(yawDifference, 45.0F) - yawDifference;
        }
        // Smooth mode: the nearest direction as it is (rounded below).

        // To the nearest of the eight directions. Truncating, a difference that
        // came out a hair under a multiple of 45 (floating point) dropped a whole
        // direction.
        int yawOrdinal = Math.floorMod(Math.round(yawDifference / 45.0F), MovementDirection.values().length);

        MovementDirection inputMoveDirection = movementInput.moveStrafe < 0.0F ? MovementDirection.EAST : MovementDirection.WEST;
        if (movementInput.moveForward > 0.0F) {
            inputMoveDirection = movementInput.moveStrafe < 0.0F ? MovementDirection.NORTH_EAST : (movementInput.moveStrafe > 0.0F ? MovementDirection.NORTH_WEST : MovementDirection.NORTH);
        }
        if (movementInput.moveForward < 0.0F) {
            inputMoveDirection = movementInput.moveStrafe < 0.0F ? MovementDirection.SOUTH_EAST : (movementInput.moveStrafe > 0.0F ? MovementDirection.SOUTH_WEST : MovementDirection.SOUTH);
        }

        return MovementDirection.values()[Math.floorMod(inputMoveDirection.ordinal() + yawOrdinal, MovementDirection.values().length)];
    }

    private enum MovementDirection {
        NORTH       (true,  false, false, false),
        NORTH_EAST  (true,  true,  false, false),
        EAST        (false, true,  false, false),
        SOUTH_EAST  (false, true,  true,  false),
        SOUTH       (false, false, true,  false),
        SOUTH_WEST  (false, false, true,  true ),
        WEST        (false, false, false, true ),
        NORTH_WEST  (true,  false, false, true );

        final boolean forward, right, back, left;

        MovementDirection(boolean forward, boolean right, boolean back, boolean left) {
            this.forward = forward;
            this.right = right;
            this.back = back;
            this.left = left;
        }
    }
}

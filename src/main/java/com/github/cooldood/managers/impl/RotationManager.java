package com.github.cooldood.managers.impl;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MovementInputEvent;
import com.github.cooldood.events.impl.MotionEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.events.impl.RotationEvent;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.MoveUtils;
import com.github.cooldood.utils.minecraft.RotationUtil;
import com.github.cooldood.utils.minecraft.RotationUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector2f;

import java.util.function.Function;

public class RotationManager {

    @Setter
    @Getter
    private static boolean active;

    @Setter
    @Getter
    private static boolean smoothed;

    public static Vector2f rotations;
    public static Vector2f lastRotations = new Vector2f(0, 0);
    public static Vector2f targetRotations;
    public static Vector2f lastServerRotations;

    private static double rotationSpeed;
    private static MovementFix correctMovement = MovementFix.OFF;
    private static Function<Vector2f, Boolean> raycast;
    private static float randomAngle;
    private static final Vector2f offset = new Vector2f(0, 0);

    public static void setRotations(float yaw, float pitch, double speed, MovementFix fix) {
        setRotations(new Vector2f(yaw, pitch), speed, fix, null);
    }

    public static void setRotations(float[] rot, double speed, MovementFix fix) {
        setRotations(new Vector2f(rot[0], rot[1]), speed, fix, null);
    }

    public static void setRotations(float yaw, float pitch, double speed, MovementFix fix, Function<Vector2f, Boolean> raycast) {
        setRotations(new Vector2f(yaw, pitch), speed, fix, raycast);
    }

    public static void setRotations(float[] rot, double speed, MovementFix fix, Function<Vector2f, Boolean> raycast) {
        setRotations(new Vector2f(rot[0], rot[1]), speed, fix, raycast);
    }

    public static void setRotations(Vector2f rot, double speed, MovementFix fix) {
        setRotations(rot, speed, fix, null);
    }

    public static void setRotations(Vector2f rot, double speed, MovementFix fix, Function<Vector2f, Boolean> raycast) {
        targetRotations = rot;
        rotationSpeed = speed * 36;
        correctMovement = fix;
        RotationManager.raycast = raycast;
        active = true;
        smooth();
    }

    public static void smooth() {
        if (!smoothed && targetRotations != null && C.p() != null) {
            float targetYaw = targetRotations.x;
            float targetPitch = targetRotations.y;
            if (raycast != null && rotations != null && (Math.abs(targetYaw - rotations.x) > 5 || Math.abs(targetPitch - rotations.y) > 5)) {
                Vector2f trueTarget = new Vector2f(targetRotations.getX(), targetRotations.getY());
                double speed = (Math.random() * Math.random() * Math.random()) * 20;
                randomAngle += (float) ((20 + (float) (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360)) * (C.p().ticksExisted / 10 % 2 == 0 ? -1 : 1));
                offset.setX(offset.getX() + -MathHelper.sin((float) Math.toRadians(randomAngle)) * (float) speed);
                offset.setY(offset.getY() + MathHelper.cos((float) Math.toRadians(randomAngle)) * (float) speed);
                targetYaw += offset.getX();
                targetPitch += offset.getY();
                if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                    randomAngle = (float) Math.toDegrees(Math.atan2(trueTarget.getX() - targetYaw, targetPitch - trueTarget.getY())) - 180;
                    targetYaw -= offset.getX();
                    targetPitch -= offset.getY();
                    offset.setX(offset.getX() + -MathHelper.sin((float) Math.toRadians(randomAngle)) * (float) speed);
                    offset.setY(offset.getY() + MathHelper.cos((float) Math.toRadians(randomAngle)) * (float) speed);
                    targetYaw += offset.getX();
                    targetPitch += offset.getY();
                }
                if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                    offset.setX(0);
                    offset.setY(0);
                    targetYaw = (float) (targetRotations.x + Math.random() * 2);
                    targetPitch = (float) (targetRotations.y + Math.random() * 2);
                }
            }
            targetYaw = lastRotations.x + MathHelper.wrapAngleTo180_float(targetYaw - lastRotations.x);
            rotations = RotationUtils.smooth(lastRotations, new Vector2f(targetYaw, targetPitch), rotationSpeed + Math.random());
            if (correctMovement == MovementFix.NORMAL || correctMovement == MovementFix.TRADITIONAL) {
                C.p().rotationYawHead = rotations.x;
            }
        }
        smoothed = true;
        if (C.mc.entityRenderer != null) {
            C.mc.entityRenderer.getMouseOver(1);
        }
    }

    @SubscribeEvent
    public static void onPreUpdate(PlayerUpdateEvent event) {
        if (C.p() == null) return;
        if (rotations == null || lastRotations == null || targetRotations == null || lastServerRotations == null) {
            rotations = new Vector2f(C.p().rotationYaw, C.p().rotationPitch);
            lastRotations = new Vector2f(C.p().rotationYaw, C.p().rotationPitch);
            targetRotations = new Vector2f(C.p().rotationYaw, C.p().rotationPitch);
            lastServerRotations = new Vector2f(C.p().rotationYaw, C.p().rotationPitch);
        }
        if (active) {
            smooth();
        }
        if (correctMovement == MovementFix.BACKWARDS_SPRINT && active && Math.abs(rotations.x % 360 - Math.toDegrees(MoveUtils.direction()) % 360) > 45) {
            net.minecraft.client.settings.KeyBinding.setKeyBindState(C.mc.gameSettings.keyBindSprint.getKeyCode(), false);
            C.p().setSprinting(false);
        }
    }

    @SubscribeEvent
    public static void onRotation(RotationEvent event) {
        if (active && rotations != null) {
            event.rotation = new RotationUtil.Rotation(rotations.y, rotations.x);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputEvent event) {
        if (active && (correctMovement == MovementFix.NORMAL || correctMovement == MovementFix.TRADITIONAL) && rotations != null) {
            MoveUtils.fixMovement(event, rotations.x);
        }
    }

    @SubscribeEvent
    public static void onMotion(MotionEvent event) {
        if (C.p() == null) return;
        if (active && rotations != null) {
            C.p().rotationYawHead = rotations.x;
            lastServerRotations = new Vector2f(rotations.x, rotations.y);
            if (Math.abs((rotations.x - C.p().rotationYaw) % 360) < 1 && Math.abs(rotations.y - C.p().rotationPitch) < 1) {
                active = false;
                correctDisabledRotations();
            }
            lastRotations = rotations;
        } else {
            lastRotations = new Vector2f(C.p().rotationYaw, C.p().rotationPitch);
        }
        targetRotations = new Vector2f(C.p().rotationYaw, C.p().rotationPitch);
        smoothed = false;
    }

    private static void correctDisabledRotations() {
        if (C.p() == null) return;
        Vector2f rot = new Vector2f(C.p().rotationYaw, C.p().rotationPitch);
        Vector2f fixed = RotationUtils.resetRotation(RotationUtils.applySensitivityPatch(rot, lastRotations));
        if (fixed != null) {
            float yawDelta = MathHelper.wrapAngleTo180_float(fixed.x - C.p().rotationYaw);
            C.p().rotationYaw += yawDelta;
            C.p().rotationPitch = fixed.y;
        }
    }

    @AllArgsConstructor
    public enum MovementFix {
        OFF("Off"),
        NORMAL("Normal"),
        TRADITIONAL("Traditional"),
        BACKWARDS_SPRINT("Backwards Sprint");

        public final String name;

        @Override
        public String toString() {
            return name;
        }
    }
}

package coolware.module.movement.scaffold;

import coolware.util.RandomUtil;
import coolware.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class Rotation {
    public static float calculateYawDiffTo180(float currentYaw, float eventYaw) {
        return RotationUtil.wrapAngleDiff(currentYaw - 180.0F, eventYaw);
    }

    public static float calculateDiagonalYaw(float currentYaw, float yawDiffTo180, float eventYaw, boolean isDiag) {
        return isDiag
                ? yawDiffTo180
                : RotationUtil.wrapAngleDiff(
                        currentYaw - 135.0F * ((currentYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F),
                        eventYaw);
    }

    public static float[] calculateModeRotation(int rotationMode, float currentYaw, float yawDiffTo180, float diagonalYaw,
                                                float existingYaw, float existingPitch, boolean onGround) {
        float targetYaw = existingYaw;
        float targetPitch = existingPitch;

        switch (rotationMode) {
            case 1: // DEFAULT
                if (existingYaw == -180.0F && existingPitch == 0.0F) {
                    targetYaw = RotationUtil.quantizeAngle(diagonalYaw);
                    targetPitch = RotationUtil.quantizeAngle(85.0F);
                } else {
                    targetYaw = RotationUtil.quantizeAngle(diagonalYaw);
                }
                break;
            case 2: // BACKWARDS
                if (existingYaw == -180.0F && existingPitch == 0.0F) {
                    targetYaw = RotationUtil.quantizeAngle(yawDiffTo180);
                    targetPitch = RotationUtil.quantizeAngle(85.0F);
                } else {
                    targetYaw = RotationUtil.quantizeAngle(yawDiffTo180);
                }
                break;
            case 3: // Hypixel
                float offsetYaw = currentYaw + 45.0F;
                if (existingYaw == -180.0F && existingPitch == 0.0F) {
                    targetYaw = RotationUtil.quantizeAngle(offsetYaw);
                    targetPitch = RotationUtil.quantizeAngle(85.0F);
                } else if (onGround) {
                    targetYaw = RotationUtil.quantizeAngle(offsetYaw);
                }
                break;
        }
        return new float[]{ targetYaw, targetPitch };
    }

    public static float[] applyTowerRotation(float eventYaw, float currentYaw, float existingTargetYaw, float existingTargetPitch,
                                             int rotationTick, float startMin, float startMax, float normalMin, float normalMax) {
        float targetYaw = existingTargetYaw;
        float yawDiff = MathHelper.wrapAngleTo180_float(currentYaw - eventYaw);

        float tolerance = rotationTick >= 2
                ? RandomUtil.nextFloat(startMin, startMax)
                : RandomUtil.nextFloat(normalMin, normalMax);

        if (Math.abs(yawDiff) > tolerance) {
            float clampedYaw = RotationUtil.clampAngle(yawDiff, tolerance);
            targetYaw = RotationUtil.quantizeAngle(eventYaw + clampedYaw);
        }
        return new float[]{ targetYaw, existingTargetPitch };
    }

    public static float[] applyTowerRotating(Minecraft mc, float eventYaw) {
        float yawDelta = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - eventYaw);
        float targetYaw = RotationUtil.quantizeAngle(eventYaw + yawDelta * RandomUtil.nextFloat(0.98F, 0.99F));
        float targetPitch = RotationUtil.quantizeAngle(RandomUtil.nextFloat(30.0F, 80.0F));
        return new float[]{ targetYaw, targetPitch };
    }

    public static float[] applyHypixelSmoothing(Minecraft mc, float targetYaw, float targetPitch, float eventYaw, float eventPitch, int rotationTick,
                                                float tellyNormalMin, float tellyNormalMax, float tellyStartMin, float tellyStartMax,
                                                float hypixelMinSpeed, float hypixelMaxSpeed) {
        if (!mc.thePlayer.onGround) {
            float rotSpeed = (mc.gameSettings != null && mc.gameSettings.keyBindJump.isKeyDown() ? 3.25F : 2.5F);
            float yawDelta = MathHelper.wrapAngleTo180_float(targetYaw - eventYaw);
            float pitchDelta = MathHelper.clamp_float(targetPitch - eventPitch, -90.0F, 90.0F);
            float minTol = Math.min(tellyNormalMin, tellyNormalMax);
            float maxTol = Math.max(tellyNormalMin, tellyNormalMax);
            float tolerance = rotationTick >= 2
                    ? RandomUtil.nextFloat(Math.min(55.0F, tellyStartMin), Math.min(68.0F, tellyStartMax))
                    : RandomUtil.nextFloat(minTol, maxTol);
            float step = yawDelta / rotSpeed;
            targetYaw = RotationUtil.quantizeAngle(eventYaw + RotationUtil.clampAngle(step, tolerance));
            targetPitch = RotationUtil.quantizeAngle(MathHelper.clamp_float(
                    eventPitch + RotationUtil.clampAngle(pitchDelta / rotSpeed, tolerance), -90.0F, 90.0F));
        } else {
            float minSpeed = Math.min(hypixelMinSpeed, hypixelMaxSpeed);
            float maxSpeed = Math.max(hypixelMinSpeed, hypixelMaxSpeed);
            float rotSpeed = RandomUtil.nextFloat(minSpeed, maxSpeed);
            if (mc.gameSettings != null && mc.gameSettings.keyBindJump.isKeyDown()) {
                rotSpeed = Math.min(180.0F, rotSpeed * 1.083F);
            }
            float yawDelta = MathHelper.wrapAngleTo180_float(targetYaw - eventYaw);
            float pitchDelta = MathHelper.clamp_float(targetPitch - eventPitch, -90.0F, 90.0F);

            targetYaw = RotationUtil.quantizeAngle(eventYaw + RotationUtil.clampAngle(yawDelta, rotSpeed));
            targetPitch = RotationUtil.quantizeAngle(MathHelper.clamp_float(
                    eventPitch + RotationUtil.clampAngle(pitchDelta, rotSpeed), -90.0F, 90.0F));
        }
        return new float[]{ targetYaw, targetPitch };
    }

    public static float[] calculateFakeClientRotation(float currentYaw, float fakeYaw, float fakePitch, float speed) {
        float targetFakeYaw = RotationUtil.wrapAngleDiff(currentYaw - 180.0F, fakeYaw);
        float targetFakePitch = 82.0F;
        float yawDiff = targetFakeYaw - fakeYaw;
        float pitchDiff = targetFakePitch - fakePitch;
        if (speed >= 180.0F) {
            return new float[]{ targetFakeYaw, targetFakePitch };
        }
        float step = Math.max(0.5F, speed * 0.12F);
        float newYaw = fakeYaw + RotationUtil.clampAngle(yawDiff, step);
        float newPitch = fakePitch + RotationUtil.clampAngle(pitchDiff, step * 0.8F);
        return new float[]{ newYaw, newPitch };
    }
}

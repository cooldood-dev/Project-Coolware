package coolware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class RotationUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static float serverYaw;
    public static float serverPitch;
    public static float prevServerYaw;
    public static float prevServerPitch;
    public static boolean silentActive = false;

    public static void setSilentRotation(float targetYaw, float targetPitch) {
        setImmediateSilentRotation(targetYaw, targetPitch);
    }

    public static void setImmediateSilentRotation(float yaw, float pitch) {
        float newPitch = MathHelper.clamp_float(pitch, -90.0F, 90.0F);
        if (silentActive) {
            float yawDiff = MathHelper.wrapAngleTo180_float(yaw - serverYaw);
            float newYaw = serverYaw + yawDiff;
            prevServerYaw = serverYaw;
            prevServerPitch = serverPitch;
            serverYaw = applyGCD(newYaw, prevServerYaw);
            serverPitch = applyGCD(newPitch, prevServerPitch);
        } else {
            float baseYaw = (mc != null && mc.thePlayer != null) ? mc.thePlayer.rotationYaw : yaw;
            float yawDiff = MathHelper.wrapAngleTo180_float(yaw - baseYaw);
            float newYaw = baseYaw + yawDiff;
            serverYaw = newYaw;
            serverPitch = newPitch;
            prevServerYaw = newYaw;
            prevServerPitch = newPitch;
        }
        silentActive = true;
    }

    public static void setSilentRotationInstant(float yaw, float pitch) {
        setImmediateSilentRotation(yaw, pitch);
    }

    public static float applyGCD(float current, float prev) {
        if (mc == null || mc.gameSettings == null) return current;
        float sens = mc.gameSettings.mouseSensitivity;
        float f = sens * 0.6F + 0.2F;
        float gcd = f * f * f * 1.2F;
        if (gcd <= 0.0001F) return current;
        float delta = current - prev;
        delta -= delta % gcd;
        return prev + delta;
    }

    public static void resetSilentRotation() {
        silentActive = false;
        if (mc != null && mc.thePlayer != null) {
            serverYaw = mc.thePlayer.rotationYaw;
            serverPitch = mc.thePlayer.rotationPitch;
            prevServerYaw = mc.thePlayer.prevRotationYaw;
            prevServerPitch = mc.thePlayer.prevRotationPitch;
        }
    }

    public static float wrapAngleDiff(float angle, float target) {
        return target + MathHelper.wrapAngleTo180_float(angle - target);
    }

    public static float clampAngle(float angle, float maxAngle) {
        maxAngle = Math.max(0.0F, Math.min(180.0F, maxAngle));
        if (angle > maxAngle) {
            angle = maxAngle;
        } else if (angle < -maxAngle) {
            angle = -maxAngle;
        }
        return angle;
    }

    public static float quantizeAngle(float angle) {
        return (float) ((double) angle - (double) angle % (double) 0.0096F);
    }

    public static float wrapAngleTo180(float angle) {
        return MathHelper.wrapAngleTo180_float(angle);
    }

    public static float applyWrap360(float currentYaw, float targetYaw) {
        float delta = targetYaw - currentYaw;
        while (delta <= -180.0F) delta += 360.0F;
        while (delta > 180.0F) delta -= 360.0F;
        return currentYaw + delta;
    }

    public static Rotation getPreviousClientRotation() {
        if (mc.thePlayer == null) return new Rotation(0.0F, 0.0F);
        return new Rotation(mc.thePlayer.prevRotationPitch, mc.thePlayer.prevRotationYaw);
    }

    public static Rotation getCurrentClientRotation() {
        if (mc.thePlayer == null) return new Rotation(0.0F, 0.0F);
        return new Rotation(mc.thePlayer.rotationPitch, mc.thePlayer.rotationYaw);
    }

    public static Rotation getServerRotation() {
        if (silentActive) {
            return new Rotation(serverPitch, serverYaw);
        }
        return getCurrentClientRotation();
    }

    public static Rotation getPreviousServerRotation() {
        if (silentActive) {
            return new Rotation(prevServerPitch, prevServerYaw);
        }
        return getPreviousClientRotation();
    }

    public static Rotation applyGcd(Rotation targetRotation) {
        return applyGcd(getServerRotation(), targetRotation);
    }

    public static Rotation applyGcd(Rotation currentRotation, Rotation targetRotation) {
        if (mc.gameSettings == null) return targetRotation;
        double f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        double sens = (f * f * f) * 8.0F;

        targetRotation.yaw = applyWrap360(currentRotation.yaw, targetRotation.yaw);
        Rotation difference = targetRotation.difference(currentRotation);

        float yawDelta = (float) MathUtil.toNearest(difference.yaw / 0.15D, sens) * 0.15F;
        float pitchDelta = (float) MathUtil.toNearest(difference.pitch / 0.15D, sens) * 0.15F;

        return new Rotation(
                MathHelper.clamp_float(currentRotation.pitch - pitchDelta, -90.0F, 90.0F),
                currentRotation.yaw - yawDelta
        );
    }

    public static Rotation getRotation(Vec3 to) {
        if (mc.thePlayer == null) return new Rotation(0.0F, 0.0F);
        return getRotation(getServerRotation(), mc.thePlayer.getPositionEyes(1.0F), to);
    }

    public static Rotation getRotation(Vec3 from, Vec3 to) {
        return getRotation(getServerRotation(), from, to);
    }

    public static Rotation getRotation(Rotation currentRotation, Vec3 from, Vec3 to) {
        return applyGcd(currentRotation, getRotationNoGcd(currentRotation, from, to));
    }

    public static Rotation getRotationNoGcd(Rotation currentRotation, Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        double dist = Math.sqrt(diff.xCoord * diff.xCoord + diff.zCoord * diff.zCoord);

        float pitch = (float) Math.toDegrees(-Math.atan2(diff.yCoord, dist));
        float yaw = (float) Math.toDegrees(Math.atan2(diff.zCoord, diff.xCoord)) - 90.0F;

        float neededPitchChange = currentRotation.pitch - pitch;
        float neededYawChange = (currentRotation.yaw % 360.0F) - yaw;

        neededYawChange = Math.abs(neededYawChange) > 180.0F
                ? -1.0F * Math.signum(neededYawChange) * (360.0F - Math.abs(neededYawChange))
                : neededYawChange;

        return new Rotation(
                currentRotation.pitch - neededPitchChange,
                currentRotation.yaw - neededYawChange
        );
    }

    public static Rotation getLimitedRotation(Rotation from, Rotation to, float maxTurnAmount) {
        Rotation rotationDifference = to.subtract(from);
        float pitchDelta = MathHelper.clamp_float(rotationDifference.pitch, -maxTurnAmount, maxTurnAmount);
        float yawDelta = MathHelper.clamp_float(rotationDifference.yaw, -maxTurnAmount, maxTurnAmount);

        return applyGcd(from, new Rotation(pitchDelta, yawDelta).add(from));
    }

    public static Rotation getSmoothRotation(Rotation from, Rotation to, float smoothing) {
        Rotation rotationDifference = to.subtract(from);
        float pitchDelta = rotationDifference.pitch / smoothing;
        float yawDelta = rotationDifference.yaw / smoothing;

        return applyGcd(from, new Rotation(pitchDelta, yawDelta).add(from));
    }

    public static Rotation getEasedRotation(Rotation from, Rotation to, EasingUtil.EasingFunctions easingFunction, double easing) {
        Rotation rotationDifference = to.subtract(from);

        float pitchDelta = (float) (rotationDifference.pitch * easingFunction.ease(easing));
        float yawDelta = (float) (rotationDifference.yaw * easingFunction.ease(easing));

        return applyGcd(from, new Rotation(pitchDelta, yawDelta).add(from));
    }

    public static EnumFacing getFacing() {
        Rotation current = getServerRotation();
        Vec3 facingVector = WorldUtil.getVectorForRotation(current.pitch, current.yaw);
        return EnumFacing.getFacingFromVector((float) facingVector.xCoord, (float) facingVector.yCoord, (float) facingVector.zCoord);
    }

    public static class Rotation {
        public float pitch;
        public float yaw;

        public Rotation(float pitch, float yaw) {
            this.pitch = pitch;
            this.yaw = yaw;
        }

        public Rotation difference(Rotation other) {
            return new Rotation(other.pitch - this.pitch, other.yaw - this.yaw);
        }

        public Rotation add(Rotation other) {
            return new Rotation(this.pitch + other.pitch, this.yaw + other.yaw);
        }

        public Rotation subtract(Rotation other) {
            return new Rotation(this.pitch - other.pitch, this.yaw - other.yaw);
        }

        @Override
        public String toString() {
            return "Rotation(" + pitch + ", " + yaw + ")";
        }
    }
}

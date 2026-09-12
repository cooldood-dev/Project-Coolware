package com.github.cooldood.utils.minecraft;

import com.github.cooldood.managers.impl.RotationManager;
import com.github.cooldood.utils.client.C;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

public class RotationUtils {

    public static float[] getRotationsTo(Vec3 from, Vec3 to) {
        double dx = to.xCoord - from.xCoord;
        double dy = to.yCoord - from.yCoord;
        double dz = to.zCoord - from.zCoord;
        double distHorizontal = MathHelper.sqrt_double(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distHorizontal));
        if (C.p() != null) {
            yaw = C.p().rotationYaw + MathHelper.wrapAngleTo180_float(yaw - C.p().rotationYaw);
            pitch = C.p().rotationPitch + MathHelper.wrapAngleTo180_float(pitch - C.p().rotationPitch);
        }
        return new float[]{yaw, pitch};
    }

    public static Vector2f calculate(EntityLivingBase target, boolean flag, double range) {
        if (C.p() == null || target == null) return new Vector2f(0, 0);
        Vec3 eyes = C.p().getPositionEyes(1.0f);
        Vec3 targetPos = new Vec3(target.posX, target.posY + target.getEyeHeight() * 0.75, target.posZ);
        float[] rot = getRotationsTo(eyes, targetPos);
        return new Vector2f(rot[0], rot[1]);
    }

    public static Vector2f calculate(Vec3 point) {
        if (C.p() == null || point == null) return new Vector2f(0, 0);
        Vec3 eyes = C.p().getPositionEyes(1.0f);
        float[] rot = getRotationsTo(eyes, point);
        return new Vector2f(rot[0], rot[1]);
    }

    public static Vector2f move(Vector2f lastRotation, Vector2f targetRotation, double speed) {
        if (speed != 0 && lastRotation != null && targetRotation != null) {
            double deltaYaw = MathHelper.wrapAngleTo180_float(targetRotation.x - lastRotation.x);
            double deltaPitch = (targetRotation.y - lastRotation.y);
            double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            if (distance == 0) return new Vector2f(0, 0);
            double distributionYaw = Math.abs(deltaYaw / distance);
            double distributionPitch = Math.abs(deltaPitch / distance);
            double maxYaw = speed * distributionYaw;
            double maxPitch = speed * distributionPitch;
            float moveYaw = (float) Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
            float movePitch = (float) Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);
            return new Vector2f(moveYaw, movePitch);
        }
        return new Vector2f(0, 0);
    }

    public static Vector2f smooth(Vector2f targetRotation, double speed) {
        Vector2f last = RotationManager.lastRotations;
        if (last == null && C.p() != null) {
            last = new Vector2f(C.p().rotationYaw, C.p().rotationPitch);
        } else if (last == null) {
            last = new Vector2f(0, 0);
        }
        return smooth(last, targetRotation, speed);
    }

    public static Vector2f smooth(Vector2f lastRotation, Vector2f targetRotation, double speed) {
        float yaw = targetRotation.x;
        float pitch = targetRotation.y;
        if (speed != 0) {
            Vector2f move = move(lastRotation, targetRotation, speed);
            yaw = lastRotation.x + move.x;
            pitch = lastRotation.y + move.y;
            Vector2f rotations = new Vector2f(yaw, pitch);
            Vector2f fixedRotations = applySensitivityPatch(rotations, lastRotation);
            yaw = fixedRotations.x;
            pitch = Math.max(-90, Math.min(90, fixedRotations.y));
        }
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f applySensitivityPatch(Vector2f rotation) {
        Vector2f last = RotationManager.lastRotations;
        if (last == null && C.p() != null) {
            last = new Vector2f(C.p().rotationYaw, C.p().rotationPitch);
        } else if (last == null) {
            last = new Vector2f(0, 0);
        }
        return applySensitivityPatch(rotation, last);
    }

    public static Vector2f applySensitivityPatch(Vector2f rotation, Vector2f previousRotation) {
        float mouseSensitivity = (float) (C.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F);
        double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        if (multiplier == 0) multiplier = 0.0001;
        float yaw = previousRotation.x + (float) (Math.round((rotation.x - previousRotation.x) / multiplier) * multiplier);
        float pitch = previousRotation.y + (float) (Math.round((rotation.y - previousRotation.y) / multiplier) * multiplier);
        return new Vector2f(yaw, MathHelper.clamp_float(pitch, -90, 90));
    }

    public static Vector2f resetRotation(Vector2f rotation) {
        if (rotation == null || C.p() == null) return null;
        float yaw = rotation.x + MathHelper.wrapAngleTo180_float(C.p().rotationYaw - rotation.x);
        float pitch = C.p().rotationPitch;
        return new Vector2f(yaw, pitch);
    }
}

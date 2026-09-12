package com.github.cooldood.utils.minecraft;

import com.github.cooldood.events.impl.MovementInputEvent;
import com.github.cooldood.utils.client.C;
import net.minecraft.util.MathHelper;

public class MoveUtils {
    public static void fixMovement(MovementInputEvent event, float yaw) {
        if (event == null || event.movementInput == null || C.p() == null) return;
        float forward = event.movementInput.moveForward;
        float strafe = event.movementInput.moveStrafe;
        if (forward == 0.0f && strafe == 0.0f) return;

        float diff = MathHelper.wrapAngleTo180_float(C.p().rotationYaw - yaw);
        double rad = Math.toRadians(diff);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float newForward = Math.round(forward * cos - strafe * sin);
        float newStrafe = Math.round(forward * sin + strafe * cos);

        event.movementInput.moveForward = newForward;
        event.movementInput.moveStrafe = newStrafe;
    }

    public static double direction() {
        if (C.p() == null) return 0.0;
        float rotationYaw = C.p().rotationYaw;
        if (C.p().moveForward < 0.0f) rotationYaw += 180.0f;
        float forward = 1.0f;
        if (C.p().moveForward < 0.0f) forward = -0.5f;
        else if (C.p().moveForward > 0.0f) forward = 0.5f;
        if (C.p().moveStrafing > 0.0f) rotationYaw -= 90.0f * forward;
        if (C.p().moveStrafing < 0.0f) rotationYaw += 90.0f * forward;
        return Math.toRadians(rotationYaw);
    }
}

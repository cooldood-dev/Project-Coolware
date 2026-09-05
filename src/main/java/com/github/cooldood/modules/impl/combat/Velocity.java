package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.bridge.net.minecraft.network.play.server.S12PacketEntityVelocityBridge;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.*;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.BlinkUtil;
import com.github.cooldood.utils.minecraft.MovementUtil;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnumAction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.Vec3;

@RegisterModule(
        name = "Velocity",
        description = "Reduces or eliminates knockback taken.",
        category = Category.COMBAT
)
public class Velocity extends Module {
    @RegisterSubModule(name = "0 Check")
    public static boolean zeroCheck = true;

    @RegisterSubModule(name = "Ignore Teleport")
    public static boolean ignoreTeleport = true;

    @RegisterSubModule(name = "Ignore Explosions")
    public static boolean ignoreExplosions = true;

    @RegisterSubModule(name = "Mode")
    public static VelocityMode velocityMode = VelocityMode.Dynamic;

    public enum VelocityMode {
        Vanilla,
        Keep,
        Delay,
        Jump_Reset,
        Dynamic
    }

    @RegisterSubModule(name = "Velocity X", parent = "Mode", modeParentString = {"Vanilla", "Keep"}, min = -2, max = 2)
    public static float xMulti = 1;
    @RegisterSubModule(name = "Velocity Y", parent = "Mode", modeParentString = {"Vanilla", "Keep"}, min = -2, max = 2)
    public static float yMulti = 1;
    @RegisterSubModule(name = "Velocity Z", parent = "Mode", modeParentString = {"Vanilla", "Keep"}, min = -2, max = 2)
    public static float zMulti = 1;

    @RegisterSubModule(name = "Max Delay Ticks", parent = "Mode", modeParentString = {"Delay", "Dynamic"}, min = 1, max = 20)
    public static int maxDelayTicks = 5;

    @RegisterSubModule(name = "Delay Outgoing", parent = "Mode", modeParentString = {"Delay", "Dynamic"})
    public static boolean delayOutgoing = false;

    @RegisterSubModule(name = "Jump", parent = "Mode", modeParentString = {"Jump_Reset", "Dynamic"})
    public static JumpResetMode jumpResetMode = JumpResetMode.Sprint_Only;

    public enum JumpResetMode {
        Force_Sprint,
        Sprint_Only,
        Normal
    }

    @RegisterSubModule(name = "Sprint FOV", parent = "Jump", modeParentString = {"Force_Sprint"}, min = 1, max = 180)
    public static int sprintFOV = 180;

    private static boolean shouldJump = false;

    private static boolean shouldDelay = false;
    private static int blinkStartTick = -1;

    private static Vec3 lastVelocity;

    private static boolean isDamage = false;

    private static boolean isDelayOutgoing = false;

    @SubscribeEvent
    public static void handleVelocityPacket(PacketEvent.Receive event) {
        if (event.isCancelled()) return;

        if (event.packet instanceof S19PacketEntityStatus && C.p() != null && C.w() != null) {
            Entity entity = ((S19PacketEntityStatus) event.packet).getEntity(C.w());
            if (entity == null || entity.getEntityId() != C.p().getEntityId()) return;

            isDamage = true;
            return;
        }

        if (event.packet instanceof S27PacketExplosion && ignoreExplosions) {
            isDamage = false;
            return;
        }

        if (!(event.packet instanceof S12PacketEntityVelocity)) return;

        S12PacketEntityVelocity packet = ((S12PacketEntityVelocity) event.packet);
        if (C.p() == null || packet.getEntityID() != C.p().getEntityId()) return;

        if (!isDamage && event.packet instanceof S12PacketEntityVelocity) {
            isDamage = true;
            return;
        }
        isDamage = !ignoreTeleport;

        S12PacketEntityVelocityBridge accessiblePacket = S12PacketEntityVelocityBridge.from(packet);
        Vec3 originalVelocity = new Vec3(packet.getMotionX()/8000d, packet.getMotionY()/8000d, packet.getMotionZ()/8000d);

        if (Math.abs(originalVelocity.xCoord) <= 0.1 && Math.abs(originalVelocity.zCoord) <= 0.1 && zeroCheck) return;

        if (!shouldDelay) lastVelocity = originalVelocity;

        switch (velocityMode) {
            case Vanilla:
                accessiblePacket.bridge$setMotionX(originalVelocity.xCoord*xMulti);
                accessiblePacket.bridge$setMotionY(originalVelocity.yCoord*yMulti);
                accessiblePacket.bridge$setMotionZ(originalVelocity.zCoord*zMulti);
                break;

            case Keep:
                accessiblePacket.bridge$setMotionX(originalVelocity.xCoord*xMulti + C.p().motionX*(1-xMulti));
                accessiblePacket.bridge$setMotionY(originalVelocity.yCoord*yMulti + C.p().motionY*(1-yMulti));
                accessiblePacket.bridge$setMotionZ(originalVelocity.zCoord*zMulti + C.p().motionZ*(1-zMulti));
                break;

            case Jump_Reset:
            case Dynamic:
                if (!C.p().isSprinting() && jumpResetMode == JumpResetMode.Sprint_Only) return;

                if (C.p().onGround || velocityMode == VelocityMode.Jump_Reset) {
                    shouldJump |= C.p().onGround;
                    break;
                }
            case Delay:
                if (shouldDelay) break;

                BlinkUtil.pushBlink(false, true, event.packet);
                event.setCancelled(true);

                if (delayOutgoing) {
                    BlinkUtil.pushBlink(true, false, null);
                    isDelayOutgoing = true;
                }

                shouldDelay = true;
                blinkStartTick = MovementUtil.ticks;
                break;
        }
    }

    private static boolean isWithinSprintFOV() {
        if (lastVelocity == null) return false;

        float yaw = getYawFromVelocity(lastVelocity.xCoord, lastVelocity.zCoord);
        return 180 - Math.abs(((C.p().rotationYaw - yaw) % 360) - 180) <= sprintFOV;
    }

    @SubscribeEvent(priority = 2000)
    public static void fixRotationForJumpReset(RotationEvent event) {
        if (!shouldJump || lastVelocity == null || jumpResetMode != JumpResetMode.Force_Sprint || !isWithinSprintFOV()) return;

        event.rotation.yaw = getYawFromVelocity(lastVelocity.xCoord, lastVelocity.zCoord);

        // using items is arguably very important, and would be annoying if cancelled.
        if (C.p().getHeldItem() != null && (C.p().getHeldItem().getItemUseAction() == EnumAction.BLOCK || !C.p().isUsingItem())) {
            MovementUtil.oneTickKeybind(C.mc.gameSettings.keyBindUseItem, false);
        }
    }

    @SubscribeEvent
    public static void jumpReset(MovementInputEvent event) {
        if (!shouldJump) return;

        if (jumpResetMode == JumpResetMode.Force_Sprint && isWithinSprintFOV()) {
            event.movementInput.moveForward = event.movementInput.sneak ? (float) 0.3D : 1;
            event.movementInput.moveStrafe = 0;
            MovementUtil.oneTickKeybind(C.mc.gameSettings.keyBindSprint, true);
        }

        event.movementInput.jump |= C.p().onGround;
        shouldJump = false;
    }

    @SubscribeEvent
    public static void finishDynamicVelocity(ClientTickEvent event) {
        if (!BlinkUtil.isBlinking(true, true) || !shouldStopBlink()) return;

        BlinkUtil.popBlink(isDelayOutgoing, true);
        isDelayOutgoing = false;
        shouldJump |= velocityMode == VelocityMode.Dynamic && C.p().onGround;
        resetVelocity();
    }

    @SubscribeEvent
    public static void resetVelocity(RespawnEvent event) {
        resetVelocity();
    }

    private static float getYawFromVelocity(double x, double z) {
        float yaw = (float) (Math.toDegrees(Math.atan2(x + z, x - z)) + 45);
        return yaw > 180 ? yaw - 360 : yaw;
    }

    private static boolean shouldStopBlink() {
        return blinkStartTick != -1 && shouldDelay && (MovementUtil.ticks - blinkStartTick > maxDelayTicks || (C.p().onGround && velocityMode == VelocityMode.Dynamic));
    }

    private static void resetVelocity() {
        blinkStartTick = -1;
        shouldDelay = false;
    }

    @Override
    public String arrayListExtraInfo() {
        return velocityMode.name();
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {
        if (blinkStartTick != -1 && C.isInGame()) {
            BlinkUtil.popBlink(isDelayOutgoing, true);
            isDelayOutgoing = false;
            resetVelocity();
        }
    }
}

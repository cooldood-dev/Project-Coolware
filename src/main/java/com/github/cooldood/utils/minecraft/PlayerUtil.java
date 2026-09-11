package com.github.cooldood.utils.minecraft;

import com.github.cooldood.events.Bus;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.*;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.combat.KillAura;
import com.github.cooldood.modules.impl.render.Freecam;
import com.github.cooldood.utils.client.C;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.Vec3;

public class PlayerUtil {
    public static boolean canAttack() {
        if (com.github.cooldood.modules.impl.combat.Watchdog.isModuleBlocking()) return false;
        return !KillAura.isBlockingSwing() || KillAura.canSwingWhileBlocking();
    }

    public static boolean attack(Entity target) {
        swingHand();
        C.mc.playerController.attackEntity(C.p(), target);
        return true;
    }

    private static int lastSwing = -1;

    public static void swingHand() {
        if (lastSwing == MovementUtil.ticks) return;

        C.p().swingItem();
        lastSwing = MovementUtil.ticks;
    }

    private static int lastUnblock = -1;
    private static boolean wasBlocking = false;

    public static int getLastUnblock() {
        if (wasBlocking && !isUsingItem()) lastUnblock = MovementUtil.ticks;
        return lastUnblock;
    }

    public static boolean isUsingItem() {
        return C.p().isUsingItem() || KillAura.isServerBlocking() || com.github.cooldood.modules.impl.combat.Watchdog.isModuleBlocking();
    }

    @SubscribeEvent(priority = 9000)
    public static void updateLastBlocking(MotionEvent event) {
        wasBlocking = isUsingItem();
    }

    // i only learnt today (4/10/25) that when using a comma between initializing variables they arnt both set to the final value
    @Getter
    private static RotationEvent previousRotationEvent, currentRotationEvent = new RotationEvent(new RotationUtil.Rotation(0,0));

    public static void setRotationEvent(RotationEvent event) {
        previousRotationEvent = currentRotationEvent;
        currentRotationEvent = event;
        Bus.post(PlayerUtil.getCurrentRotationEvent());
    }

    public static RotationUtil.Rotation lastRotation() {
        return previousRotationEvent == null ? currentRotationEvent.rotation : previousRotationEvent.rotation;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        previousRotationEvent = null;
    }

    public static RotationUtil.Rotation currentRotation() {
        return currentRotationEvent.rotation;
    }

    public static float prevServerRenderYawOffset;
    public static float serverRenderYawOffset;

    public static MotionEvent motionEvent;

    public static boolean noClip = false;
    public static boolean noClipRender = false;

    public static Vec3 prevFakeCameraPos, fakeCameraPos, realPos;
    public static RotationUtil.Rotation fakeRotation, realRotation;

    public static boolean isRenderingGuiInventory = false;

    @SubscribeEvent
    public static void onClientTickEvent(ClientTickEvent event) {
        if (!C.isInGame() || C.p().isDead) {
            PlayerUtil.setFakeCameraPos(null);
            PlayerUtil.fakeRotation = null;
        }
    }

    @SubscribeEvent(priority = 0)
    public static void resetBlink(PacketEvent.Receive event) {
        if (event.packet instanceof S07PacketRespawn || event.packet instanceof S01PacketJoinGame || event.packet instanceof S40PacketDisconnect) {
            Bus.post(new RespawnEvent());
        }
    }

    public static void setFakeCameraPos(Vec3 pos) {
        prevFakeCameraPos = pos == null ? null : fakeCameraPos;
        fakeCameraPos = pos;
    }

    public static boolean shouldFixPlayerFakeLook() {
        return !ModuleManager.isEnabled(Freecam.class) || !Freecam.allowInteract;
    }

    public static void fakePlayerPosAndRot() {
        if (!C.isInGame() || C.p().isDead) {
            PlayerUtil.realPos = null;
            return;
        }

        if (PlayerUtil.realPos != null) {
            MovementUtil.setPrevPos(PlayerUtil.realPos);
            MovementUtil.setPos(PlayerUtil.realPos);
            PlayerUtil.realPos = null;
        }

        if (PlayerUtil.fakeRotation != null) {
            PlayerUtil.fakeRotation = RotationUtil.getCurrentClientRotation();

            C.p().rotationYaw = PlayerUtil.realRotation.yaw;
            C.p().rotationPitch = PlayerUtil.realRotation.pitch;
        }
        else if (PlayerUtil.realRotation != null) {
            C.p().rotationYaw = PlayerUtil.realRotation.yaw;
            C.p().rotationPitch = PlayerUtil.realRotation.pitch;

            PlayerUtil.realRotation = null;
        }
    }

    public static void resetFakePlayerPosAndRot() {
        if (PlayerUtil.prevFakeCameraPos != null && PlayerUtil.realPos == null) {
            PlayerUtil.realPos = C.p().getPositionVector();
            MovementUtil.setPrevPos(PlayerUtil.prevFakeCameraPos);
            MovementUtil.setPos(PlayerUtil.fakeCameraPos);
        }

        if (PlayerUtil.fakeRotation != null) {
            C.p().prevRotationYawHead = C.p().rotationYawHead = C.p().prevRotationYaw = C.p().rotationYaw = PlayerUtil.fakeRotation.yaw;
            C.p().prevRotationPitch = C.p().rotationPitch = PlayerUtil.fakeRotation.pitch;
        }
    }

    // shut up intellij, this shouldn't be inverted, because the name would be longer and its alr too long!
    public static boolean shouldInteractFromFakePos() {
        return Freecam.allowInteract && ModuleManager.isEnabled(Freecam.class) && C.isInGame() && !C.p().isDead;
    }

    public static boolean movingTowardsEntity(EntityLivingBase entity) {
        double prevDistance = new Vec3(C.p().prevPosX, C.p().prevPosY, C.p().prevPosZ).distanceTo(entity.getPositionVector());
        double currentDistance = C.p().getPositionVector().distanceTo(entity.getPositionVector());
        return currentDistance < prevDistance;
    }

    public static int ticksUntilInRange(EntityLivingBase entity, double range) {
        Vec3 playerPos = C.p().getPositionVector();
        Vec3 targetPos = entity.getPositionVector();

        double startDistance = playerPos.distanceTo(targetPos);
        playerPos = playerPos.add(new Vec3(playerPos.xCoord - C.p().prevPosX, 0, playerPos.zCoord - C.p().prevPosZ));
        targetPos = targetPos.add(new Vec3(targetPos.xCoord - entity.lastTickPosX, 0, targetPos.zCoord - entity.lastTickPosZ));
        double nextDistance = playerPos.distanceTo(targetPos);
        double distanceChange = startDistance - nextDistance;

        if (distanceChange <= 0) return -1;
        return (int) Math.ceil((startDistance - range) / distanceChange);
    }
}

package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.ClickMouseEvent;
import com.github.cooldood.events.impl.MotionEvent;
import com.github.cooldood.events.impl.MovementInputEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.events.impl.RotationEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.PacketUtil;
import com.github.cooldood.utils.minecraft.RotationUtil;
import lombok.Getter;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * KillAura - recoded from LiquidBounce''s ModuleKillAura.kt.
 * Features:
 *  - Target selection (players, mobs, animals, etc.)
 *  - GCD-normalized rotations (anti-cheat bypass)
 *  - Movement correction (Strict/Silent/ChangeLook)
 *  - CPS-based attack timing
 *  - Auto-block integration
 */
@RegisterModule(
        name = "Kill Aura",
        description = "Recoded KillAura with GCD-normalized rotations and CPS-based attack timing.",
        category = Category.COMBAT
)
public class KillAura extends Module {

    // SETTINGS
    @RegisterSubModule(name = "Range", min = 3.0, max = 6.0, increment = 0.1)
    public static double range = 3.0;

    @RegisterSubModule(name = "Block Range", min = 3.0, max = 8.0, increment = 0.1)
    public static double blockRange = 5.0;

    @RegisterSubModule(name = "CPS", min = 1.0, max = 20.0, increment = 1.0)
    public static double cps = 12.0;

    @RegisterSubModule(name = "Auto Block")
    public static boolean autoBlock = true;

    @RegisterSubModule(name = "Keep Sprint")
    public static boolean keepSprint = true;

    @RegisterSubModule(name = "Through Walls")
    public static boolean throughWalls = false;

    @RegisterSubModule(name = "Target Players")
    public static boolean targetPlayers = true;

    @RegisterSubModule(name = "Target Mobs")
    public static boolean targetMobs = true;

    @RegisterSubModule(name = "Target Animals")
    public static boolean targetAnimals = false;

    @RegisterSubModule(name = "Target Invisibles")
    public static boolean targetInvisibles = false;

    @RegisterSubModule(name = "Rotation Mode")
    public static RotationMode rotationMode = RotationMode.Normal;
    public enum RotationMode { Normal, Silent }

    @RegisterSubModule(name = "Move Fix")
    public static MoveFix moveFix = MoveFix.Silent;
    public enum MoveFix { Off, Strict, Silent, ChangeLook }

    @RegisterSubModule(name = "GCD Fix")
    public static boolean gcdFix = true;

    // STATE
    @Getter public static EntityLivingBase target = null;
    private static final List<EntityLivingBase> targets = new ArrayList<>();
    private static long lastAttackTime = 0L;
    private static float serverYaw, serverPitch;
    private static boolean shouldBlock = false;
    private static float gcd = 0.0f;

    // EVENTS
    @SubscribeEvent
    public static void onRotation(RotationEvent event) {
        updateTargets();
        target = findBestTarget();

        if (target == null) {
            serverYaw   = C.p().rotationYaw;
            serverPitch = C.p().rotationPitch;
            return;
        }

        float[] rot = getRotationToEntity(target);
        float yaw   = rot[0];
        float pitch = rot[1];

        if (gcdFix) {
            yaw   = normalizeYaw(yaw);
            pitch = normalizePitch(pitch);
        }

        serverYaw   = yaw;
        serverPitch = pitch;

        if (rotationMode == RotationMode.Normal) {
            event.rotation = new RotationUtil.Rotation(pitch, yaw);
        }
    }

    @SubscribeEvent
    public static void onMotion(MotionEvent event) {
        if (target == null) return;
        if (canAttack())  attackTarget();
        if (autoBlock)    handleAutoBlock();
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputEvent event) {
        if (target == null || moveFix == MoveFix.Off) return;
        float yaw = serverYaw;
        if (moveFix == MoveFix.Strict) {
            C.p().rotationYaw     = yaw;
            C.p().rotationYawHead = yaw;
        } else if (moveFix == MoveFix.Silent) {
            fixMovementSilent(event, yaw);
        } else if (moveFix == MoveFix.ChangeLook) {
            C.p().rotationYaw     = yaw;
            C.p().rotationYawHead = yaw;
            fixMovementSilent(event, yaw);
        }
    }

    @SubscribeEvent
    public static void onPlayerUpdate(PlayerUpdateEvent event) {
        if (target != null && keepSprint && C.p() != null) {
            C.p().setSprinting(true);
        }
    }

    @SubscribeEvent
    public static void clickMouseEvent(ClickMouseEvent.Left event) {
        if (target != null && canAttack()) {
            attackTarget();
        }
    }

    // TARGET SELECTION
    private static void updateTargets() {
        targets.clear();
        if (C.w() == null || C.p() == null) return;
        for (Object o : C.w().loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase entity = (EntityLivingBase) o;
            if (!isValidTarget(entity)) continue;
            if (C.p().getDistanceToEntity(entity) <= range) targets.add(entity);
        }
        targets.sort(Comparator.comparingDouble(e -> C.p().getDistanceToEntity(e)));
    }

    public static boolean isValidTarget(EntityLivingBase entity) {
        if (entity == C.p()) return false;
        if (entity.isDead || entity.getHealth() <= 0) return false;
        if (entity instanceof EntityArmorStand) return false;
        if (!targetInvisibles && entity.isInvisible()) return false;
        if (entity instanceof EntityPlayer && !targetPlayers) return false;
        if (entity instanceof EntityMob    && !targetMobs)    return false;
        if (entity instanceof EntityAnimal && !targetAnimals) return false;
        return true;
    }

    private static EntityLivingBase findBestTarget() {
        for (EntityLivingBase entity : targets) {
            if (throughWalls || C.p().canEntityBeSeen(entity)) return entity;
        }
        return targets.isEmpty() ? null : targets.get(0);
    }

    // ROTATION CALCULATION
    private static float[] getRotationToEntity(EntityLivingBase entity) {
        double x = entity.posX - C.p().posX;
        double y = (entity.posY + entity.getEyeHeight()) - (C.p().posY + C.p().getEyeHeight());
        double z = entity.posZ - C.p().posZ;
        float yaw   = (float)(Math.toDegrees(Math.atan2(z, x))) - 90f;
        float pitch = (float)-(Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z))));
        return new float[]{ wrapAngle(yaw), MathHelper.clamp_float(wrapAngle(pitch), -90f, 90f) };
    }

    private static float wrapAngle(float angle) {
        angle %= 360f;
        if (angle >= 180f)  angle -= 360f;
        if (angle < -180f)  angle += 360f;
        return angle;
    }

    // GCD FIX
    private static float normalizeYaw(float targetYaw) {
        if (gcd == 0f) computeGcd();
        float delta   = wrapAngle(targetYaw - C.p().rotationYaw);
        float rounded = Math.round(delta / gcd) * gcd;
        return wrapAngle(C.p().rotationYaw + rounded);
    }

    private static float normalizePitch(float targetPitch) {
        if (gcd == 0f) computeGcd();
        float cur   = C.p().rotationPitch;
        float delta = targetPitch - cur;
        delta = MathHelper.clamp_float(delta, -90f - cur, 90f - cur);
        float rounded = Math.round(delta / gcd) * gcd;
        return MathHelper.clamp_float(cur + rounded, -90f, 90f);
    }

    private static void computeGcd() {
        float s = C.mc.gameSettings.mouseSensitivity;
        float f = s * 0.6f + 0.2f;
        gcd = f * f * f * 1.2f;
    }

    // MOVEMENT CORRECTION
    private static void fixMovementSilent(MovementInputEvent event, float yaw) {
        float forward = event.movementInput.moveForward;
        float strafe  = event.movementInput.moveStrafe;
        if (forward == 0f && strafe == 0f) return;
        float movementAngle = (float) Math.toDegrees(Math.atan2(strafe, forward));
        float deltaYaw      = wrapAngle(yaw - movementAngle - C.p().rotationYaw);
        double rad = Math.toRadians(deltaYaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        event.movementInput.moveForward = (float)(forward * cos - strafe * sin);
        event.movementInput.moveStrafe  = (float)(forward * sin + strafe * cos);
    }

    // ATTACK LOGIC
    private static boolean canAttack() {
        if (target == null || C.p() == null) return false;
        if (System.currentTimeMillis() - lastAttackTime < (long)(1000.0 / cps)) return false;
        if (C.p().getDistanceToEntity(target) > range) return false;
        if (C.mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) return false;
        return isValidTarget(target);
    }

    private static void attackTarget() {
        if (target == null) return;
        if (autoBlock) block();
        C.mc.playerController.attackEntity(C.p(), target);
        C.p().swingItem();
        lastAttackTime = System.currentTimeMillis();
        if (autoBlock && !shouldBlock) unblock();
    }

    // AUTO-BLOCK
    private static void handleAutoBlock() {
        if (target == null || C.p() == null) return;
        if (C.p().getDistanceToEntity(target) <= blockRange) {
            shouldBlock = true;
            block();
        } else {
            shouldBlock = false;
            unblock();
        }
    }

    private static boolean isHoldingSword() {
        ItemStack held = C.p() != null ? C.p().getHeldItem() : null;
        return held != null && held.getItem() instanceof ItemSword;
    }

    private static void block() {
        if (isHoldingSword()) {
            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(C.p().getHeldItem()));
        }
    }

    private static void unblock() {
        if (C.p() == null) return;
        PacketUtil.sendPacket(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
    }

    // LIFECYCLE
    @Override
    protected void onEnable() {
        target         = null;
        lastAttackTime = 0L;
        gcd            = 0f;
        shouldBlock    = false;
    }

    @Override
    protected void onDisable() {
        target      = null;
        shouldBlock = false;
        if (C.p() != null) unblock();
    }

    // COMPATIBILITY API FOR BUS, MIXINS, PLAYERUTIL, FAKELAG, TICKBASE
    public enum AutoBlockMode {
        Watchdog, Fake, Vanilla, NCP, Legit, Predictive, None, Basic, BlocksMC, Verus
    }
    public static AutoBlockMode autoblockMode = AutoBlockMode.Watchdog;
    public static boolean swingQueued = false;
    public static boolean clickBlockQueued = false;

    public static boolean isBlocking() {
        return shouldBlock;
    }

    public static boolean isServerBlocking() {
        return shouldBlock;
    }

    public static boolean canSwingWhileBlocking() {
        return true;
    }

    public static boolean isBlockingSwing() {
        return shouldBlock;
    }

    // Bus compatibility hooks
    public static void tickAutoBlock(PlayerUpdateEvent event) {}
    public static void tickSwingQueued(PlayerUpdateEvent event) {}
    public static void tryAttackTarget(PlayerUpdateEvent event) {}
    public static void onPlayerMotion(MotionEvent event) {}
    public static void onRotationEvent(RotationEvent event) {
        onRotation(event);
    }

    private static final SecureRandom random = new SecureRandom();
}

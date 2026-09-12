package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MotionEvent;
import com.github.cooldood.events.impl.MovementInputEvent;
import com.github.cooldood.events.impl.RespawnEvent;
import com.github.cooldood.events.impl.RotationEvent;
import com.github.cooldood.events.impl.WorldUnloadEvent;
import com.github.cooldood.managers.impl.PolarRotationManager;
import com.github.cooldood.managers.impl.RotationLearnerManager;
import com.github.cooldood.managers.impl.RotationManager;
import com.github.cooldood.managers.impl.TargetManager;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.*;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@RegisterModule(
        name = "Kill Aura",
        description = "Automatically attacks entities with advanced procedural, ML, and smoothed rotations.",
        category = Category.COMBAT
)
public class KillAura extends Module {

    @RegisterSubModule(name = "Mode")
    public static Mode mode = Mode.Adaptive;

    public enum Mode {
        Single,
        Switch,
        Adaptive
    }

    @RegisterSubModule(name = "Seek Range", min = 3.0, max = 8.0, increment = 0.1)
    public static double seekRange = 6.0;

    @RegisterSubModule(name = "Attack Range", min = 3.0, max = 6.0, increment = 0.1)
    public static double attackRange = 4.2;

    @RegisterSubModule(name = "Swing Range", min = 3.0, max = 8.0, increment = 0.1)
    public static double swingRange = 4.5;

    @RegisterSubModule(name = "Block Range", min = 3.0, max = 8.0, increment = 0.1)
    public static double blockRange = 5.0;

    @RegisterSubModule(name = "Auto Block")
    public static AutoBlock ab = AutoBlock.Fake;

    public enum AutoBlock {
        Fake,
        None
    }

    @RegisterSubModule(name = "Min CPS", min = 1.0, max = 20.0, increment = 1.0)
    public static double min = 8.0;

    @RegisterSubModule(name = "Max CPS", min = 1.0, max = 20.0, increment = 1.0)
    public static double max = 12.0;

    @RegisterSubModule(name = "Use Only Mouse", description = "Simulate mouse clicks instead of packets")
    public static boolean useOnlyMouse = false;

    @RegisterSubModule(name = "Through Walls")
    public static boolean throughWalls = false;

    @RegisterSubModule(name = "Rotations")
    public static Rotations rotations = Rotations.Normal;

    public enum Rotations {
        Normal,
        ML,
        Polar,
        None
    }

    @RegisterSubModule(name = "Min Rot Speed", min = 0.0, max = 10.0, increment = 0.5)
    public static double minRotSpeed = 3.0;

    @RegisterSubModule(name = "Max Rot Speed", min = 0.0, max = 10.0, increment = 0.5)
    public static double maxRotSpeed = 7.0;

    @RegisterSubModule(name = "Body Ease", min = 0.01, max = 1.0, increment = 0.01)
    public static double bodyEase = 0.2;

    @RegisterSubModule(name = "ML Ease", min = 0.01, max = 1.0, increment = 0.01)
    public static double mlEase = 0.2;

    @RegisterSubModule(name = "Advanced Polar")
    public static boolean advancedPolar = false;

    @RegisterSubModule(name = "Polar Noise Scale", min = 0.5, max = 8.0, increment = 0.1)
    public static double polarNoiseScale = 2.0;

    @RegisterSubModule(name = "Polar Warp Strength", min = 0.1, max = 5.0, increment = 0.1)
    public static double polarWarpStrength = 1.5;

    @RegisterSubModule(name = "Polar Noise Octaves", min = 1.0, max = 6.0, increment = 1.0)
    public static double polarNoiseOctaves = 3.0;

    @RegisterSubModule(name = "Polar Flick Chance", min = 0.0, max = 100.0, increment = 1.0)
    public static double polarFlickChance = 50.0;

    @RegisterSubModule(name = "Ray Cast")
    public static boolean rayCast = false;

    @RegisterSubModule(name = "Move Fix")
    public static MoveFix fix = MoveFix.Silent;

    public enum MoveFix {
        None,
        Strict,
        Silent
    }

    @RegisterSubModule(name = "Keep Sprint")
    public static boolean sprint = false;

    @RegisterSubModule(name = "Hypixel Sprint", parent = "Keep Sprint")
    public static boolean hypixelSprint = false;

    @RegisterSubModule(name = "Auto Disable")
    public static boolean autoDisable = true;

    @RegisterSubModule(name = "Target Players")
    public static boolean targetPlayers = true;

    @RegisterSubModule(name = "Target Teammates")
    public static boolean targetTeammates = true;

    @RegisterSubModule(name = "Target Invisibles")
    public static boolean targetInvisibles = false;

    @RegisterSubModule(name = "Target Mobs")
    public static boolean targetMobs = true;

    @RegisterSubModule(name = "Target Animals")
    public static boolean targetAnimals = false;

    @Getter
    public static EntityLivingBase target;
    public static boolean autoBlocking = false;
    public static boolean canAttack = true;
    public static final TimerUtils attackTimer = new TimerUtils();
    public static long delay = 0;
    public static int hitTicks = 0;

    private static EntityLivingBase lastTarget;
    private static Vec3 smoothedBodyPoint;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    protected void onEnable() {
        delay = (long) (1000.0 / getCPS());
        canAttack = true;
        autoBlocking = false;
        syncTargetManagerConfig();
        attackTimer.reset();
        if (rotations == Rotations.ML && !RotationLearnerManager.hasModelLoaded()) {
            LoggingUtils.sendChatMessage("ML model not loaded, use .rot load <name> to load a rotation model!");
        }
    }

    @Override
    protected void onDisable() {
        resetCombatState();
    }

    public static void resetCombatState() {
        if (autoBlocking) {
            unblock();
        } else {
            canAttack = true;
        }
        target = null;
        lastTarget = null;
        smoothedBodyPoint = null;
        RotationLearnerManager.resetSmoothing();
        PolarRotationManager.reset();
        RotationManager.setActive(false);
        delay = 0;
        attackTimer.reset();
    }

    private static double getCPS() {
        double minVal = min;
        double maxVal = max;
        if (maxVal <= 0) maxVal = 1.0;
        if (minVal < 0) minVal = 0.0;
        if (minVal > maxVal) {
            double temp = minVal;
            minVal = maxVal;
            maxVal = temp;
        }
        double cpsVal = MathHelper.clamp_double(minVal + (maxVal - minVal) * SECURE_RANDOM.nextDouble(), minVal, maxVal);
        return Math.max(1.0, cpsVal);
    }

    private static void syncTargetManagerConfig() {
        List<TargetManager.Targets> list = new ArrayList<>();
        if (targetPlayers) list.add(TargetManager.Targets.PLAYERS);
        if (targetTeammates) list.add(TargetManager.Targets.TEAMMATES);
        if (targetInvisibles) list.add(TargetManager.Targets.INVISIBLES);
        if (targetMobs) list.add(TargetManager.Targets.HOSTILES);
        if (targetAnimals) list.add(TargetManager.Targets.ANIMALS);
        TargetManager.configure(list);
        TargetManager.setSeekRange((float) seekRange);
        if (mode == Mode.Single) TargetManager.setMode(TargetManager.Mode.SINGLE);
        else if (mode == Mode.Switch) TargetManager.setMode(TargetManager.Mode.SWITCH);
        else TargetManager.setMode(TargetManager.Mode.ADAPTIVE);
    }

    @SubscribeEvent
    public static void onRotation(RotationEvent event) {
        if (C.p() == null || C.w() == null) {
            if (target != null || autoBlocking) resetCombatState();
            return;
        }

        syncTargetManagerConfig();
        TargetManager.updateTargets();
        target = TargetManager.getTarget();

        if (target != null && !throughWalls && !canSeeEntity(target)) {
            target = null;
        }

        if (target == null) {
            unblock();
            canAttack = true;
            return;
        }

        calculateRotations();

        if (RotationManager.rotations != null) {
            event.rotation = new RotationUtil.Rotation(RotationManager.rotations.y, RotationManager.rotations.x);
        }

        if (ab != AutoBlock.None && getDistanceToTarget(target) <= blockRange && InvUtils.isHoldingSword()) {
            autoblock();
        }

        attack();
    }

    @SubscribeEvent
    public static void onMotion(MotionEvent event) {
        hitTicks++;
        if (target != null && canAttack) {
            attack();
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputEvent event) {
        if (target == null || fix == MoveFix.None || !RotationManager.isActive() || RotationManager.rotations == null) return;
        float yaw = RotationManager.rotations.x;
        if (fix == MoveFix.Strict) {
            C.p().rotationYaw = yaw;
            C.p().rotationYawHead = yaw;
        } else if (fix == MoveFix.Silent) {
            MoveUtils.fixMovement(event, yaw);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldUnloadEvent event) {
        resetCombatState();
        if (autoDisable) {
            ModuleManager.setEnabled(KillAura.class, false);
        }
    }

    @SubscribeEvent
    public static void onRespawn(RespawnEvent event) {
        resetCombatState();
        if (autoDisable) {
            ModuleManager.setEnabled(KillAura.class, false);
        }
    }

    private static void calculateRotations() {
        if (C.p() == null || target == null || rotations == Rotations.None) return;

        if (target != lastTarget) {
            smoothedBodyPoint = null;
            RotationLearnerManager.resetSmoothing();
            PolarRotationManager.reset();
            lastTarget = target;
        }

        float rotSpeed = (float) MathUtils.getRandom(minRotSpeed, maxRotSpeed);
        Vector2f rotation;

        if (rotations == Rotations.Polar) {
            rotation = PolarRotationManager.getPolarRotation(
                    target,
                    polarNoiseScale,
                    polarWarpStrength,
                    (int) polarNoiseOctaves,
                    (float) polarFlickChance,
                    seekRange
            );
            if (rotation == null) {
                rotation = RotationUtils.calculate(target, false, seekRange);
            }
        } else if (rotations == Rotations.ML && RotationLearnerManager.hasModelLoaded()) {
            rotation = RotationLearnerManager.humanize(getWholeBodyRotation(target), 1.0f, (float) mlEase);
        } else {
            rotation = RotationUtils.calculate(target, false, seekRange);
        }

        RotationManager.MovementFix fixMode = (fix != MoveFix.None) ?
                (fix == MoveFix.Silent ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.TRADITIONAL) :
                RotationManager.MovementFix.OFF;

        RotationManager.setRotations(rotation, rotSpeed, fixMode);
    }

    private static Vector2f getWholeBodyRotation(EntityLivingBase entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double targetX = box.minX + (box.maxX - box.minX) * MathUtils.getRandom(0.0, 1.0);
        double targetY = box.minY + (box.maxY - box.minY) * MathUtils.getRandom(0.0, 1.0);
        double targetZ = box.minZ + (box.maxZ - box.minZ) * MathUtils.getRandom(0.0, 1.0);
        Vec3 desired = new Vec3(targetX, targetY, targetZ);

        if (smoothedBodyPoint == null) {
            smoothedBodyPoint = desired;
        } else {
            double ease = bodyEase;
            smoothedBodyPoint = new Vec3(
                    smoothedBodyPoint.xCoord + (desired.xCoord - smoothedBodyPoint.xCoord) * ease,
                    smoothedBodyPoint.yCoord + (desired.yCoord - smoothedBodyPoint.yCoord) * ease,
                    smoothedBodyPoint.zCoord + (desired.zCoord - smoothedBodyPoint.zCoord) * ease
            );
        }

        Vec3 eyePos = new Vec3(C.p().posX, C.p().posY + C.p().getEyeHeight(), C.p().posZ);
        float[] rot = RotationUtils.getRotationsTo(eyePos, smoothedBodyPoint);
        return new Vector2f(rot[0], rot[1]);
    }

    public static double getDistanceToTarget(EntityLivingBase target) {
        if (C.p() == null || target == null) return 999.0;
        Vec3 eyes = C.p().getPositionEyes(1.0f);
        AxisAlignedBB box = target.getEntityBoundingBox();
        double closestX = MathHelper.clamp_double(eyes.xCoord, box.minX, box.maxX);
        double closestY = MathHelper.clamp_double(eyes.yCoord, box.minY, box.maxY);
        double closestZ = MathHelper.clamp_double(eyes.zCoord, box.minZ, box.maxZ);
        return Math.min(C.p().getDistanceToEntity(target), eyes.distanceTo(new Vec3(closestX, closestY, closestZ)));
    }

    private static void autoblock() {
        if (C.p() == null || C.mc.playerController == null) return;
        if (target == null || getDistanceToTarget(target) > blockRange || !InvUtils.isHoldingSword()) {
            if (autoBlocking) unblock();
            return;
        }
        autoBlocking = true;
    }

    private static void unblock() {
        if (!autoBlocking) {
            canAttack = true;
            return;
        }
        autoBlocking = false;
        canAttack = true;
    }

    private static void attack() {
        if (C.p() == null || C.mc.playerController == null || target == null || !canAttack) return;
        if (!hitTimerDone()) return;

        double dist = getDistanceToTarget(target);
        if (dist <= attackRange && !useOnlyMouse) {
            if (rayCast) {
                Vector2f rot = RotationManager.rotations != null ? RotationManager.rotations : new Vector2f(C.p().rotationYaw, C.p().rotationPitch);
                MovingObjectPosition mop = RayCastUtils.rayCast(rot, Math.max(attackRange, blockRange));
                if (mop == null || mop.entityHit == null || mop.entityHit != target) {
                    return;
                }
            }
            C.p().swingItem();
            C.mc.playerController.attackEntity(C.p(), target);
            if (rotations == Rotations.Polar) {
                PolarRotationManager.triggerFlick((float) polarFlickChance);
            }
            if (sprint) {
                if (!hypixelSprint) {
                    C.p().setSprinting(true);
                } else if (!C.p().isCollidedHorizontally && C.p().isSprinting() && C.p().moveForward > 0 && C.p().hurtTime <= 4) {
                    C.p().setSprinting(true);
                }
            }
            hitTicks = 0;
        } else if (dist <= swingRange) {
            C.p().swingItem();
            hitTicks = 0;
        }
    }

    private static boolean hitTimerDone() {
        if (attackTimer.hasTimeElapsed(delay, false)) {
            attackTimer.reset();
            delay = (long) (1000.0 / getCPS());
            return true;
        }
        return false;
    }

    private static boolean canSeeEntity(Entity entity) {
        if (throughWalls) return true;
        if (C.p() == null || C.w() == null || entity == null) return false;
        if (C.p().canEntityBeSeen(entity)) return true;
        Vec3 eyes = C.p().getPositionEyes(1.0f);
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        Vec3 head = new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        Vec3 chest = new Vec3(entity.posX, (bb.minY + bb.maxY) / 2.0, entity.posZ);
        Vec3 feet = new Vec3(entity.posX, bb.minY + 0.1, entity.posZ);
        return C.w().rayTraceBlocks(eyes, head, false, false, false) == null
                || C.w().rayTraceBlocks(eyes, chest, false, false, false) == null
                || C.w().rayTraceBlocks(eyes, feet, false, false, false) == null;
    }

    @Override
    public String arrayListExtraInfo() {
        return mode.name();
    }
}

package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.bridge.net.minecraft.client.MinecraftBridge;
import com.github.cooldood.bridge.net.minecraft.client.multiplayer.PlayerControllerMPBridge;
import com.github.cooldood.bridge.net.minecraft.client.settings.KeyBindingBridge;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.ClickMouseEvent;
import com.github.cooldood.events.impl.MotionEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.events.impl.RotationEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.modules.impl.player.Fucker;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.MathUtil;
import com.github.cooldood.utils.minecraft.*;
import com.github.cooldood.utils.render.EasingUtil;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RegisterModule(
        name = "Kill Aura",
        description = "Provides advanced Kill Aura functionality ported from Augustus.",
        category = Category.COMBAT
)
public class KillAura extends Module {

    // ─── Mode ─────────────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Mode")
    public static AuraMode mode = AuraMode.Advanced;
    public enum AuraMode {
        Basic, Advanced
    }

    // ─── Range ────────────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Range")
    public SubCategory rangeSubcategory = new SubCategory();

    @RegisterSubModule(name = "Aim Range", parent = "Range", min = 1, max = 15, increment = 0.1)
    public static double preRange = 4.0;

    @RegisterSubModule(name = "Attack Range", parent = "Range", min = 1, max = 6, increment = 0.05)
    public static double killAuraAttackRange = 3.1;

    @RegisterSubModule(name = "FOV", parent = "Range", min = 1, max = 360, increment = 1.0)
    public static double FOV = 360;

    @RegisterSubModule(name = "Through Walls", parent = "Range")
    public static boolean throughWalls = false;

    @RegisterSubModule(name = "Haze Range", parent = "Range")
    public static boolean hazeRange = false;
    @RegisterSubModule(name = "Haze Add", parent = "Haze Range", min = 0.0, max = 1.0, increment = 0.05)
    public static double hazeAdd = 0.5;
    @RegisterSubModule(name = "Haze Max", parent = "Haze Range", min = 3.0, max = 6.0, increment = 0.1)
    public static double hazeMax = 4.5;

    // ─── Targeting ────────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Targeting")
    public SubCategory killAuraTargetingSubCategory = new SubCategory();

    @RegisterSubModule(name = "Target Sort", parent = "Targeting")
    public static KillAuraSorting killAuraSorting = KillAuraSorting.Distance;
    public enum KillAuraSorting {
        Distance, Health, Hurt_Time, FOV, Best, UltimateSwitch
    }

    @RegisterSubModule(name = "Target Choice", parent = "Targeting")
    public static KillAuraTargeting killAuraTarget = KillAuraTargeting.Best;
    public enum KillAuraTargeting {
        Switch, Best, Single
    }

    @RegisterSubModule(name = "Target Switch Delay", parent = "Targeting", min = 0, max = 1000, increment = 10)
    public static double targetDelay = 100.0;

    @RegisterSubModule(name = "Smart Aim", parent = "Targeting")
    public static boolean smartAim = true;

    @RegisterSubModule(name = "Best Hit Vec", parent = "Targeting")
    public static boolean bestHitVec = true;

    @RegisterSubModule(name = "Target Players", parent = "Targeting")
    public static boolean targetPlayers = true;

    @RegisterSubModule(name = "Target Mobs", parent = "Targeting")
    public static boolean targetMobs = true;

    @RegisterSubModule(name = "Target Animals", parent = "Targeting")
    public static boolean targetAnimals = true;

    @RegisterSubModule(name = "Target Villagers", parent = "Targeting")
    public static boolean targetVillagers = true;

    @RegisterSubModule(name = "Target Inorganics", parent = "Targeting")
    public static boolean targetArmorStands = false;

    @RegisterSubModule(name = "Target Invisibles", parent = "Targeting")
    public static boolean targetInvisibles = true;

    // ─── Rotation ─────────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Rotation")
    public SubCategory rotationsSubcategory = new SubCategory();

    @RegisterSubModule(name = "Rotation Mode", parent = "Rotation")
    public static KillAuraRotations rotations = KillAuraRotations.Augustus;
    public enum KillAuraRotations {
        Augustus, Simple, Smooth, Eased, Snap, None
    }

    @RegisterSubModule(name = "Min Yaw Speed", parent = "Rotation Mode", modeParentString = {"Augustus", "Simple"}, min = 1, max = 180, increment = 1.0)
    public static float minYawSpeed = 40;

    @RegisterSubModule(name = "Max Yaw Speed", parent = "Rotation Mode", modeParentString = {"Augustus", "Simple"}, min = 1, max = 180, increment = 1.0)
    public static float maxYawSpeed = 60;

    @RegisterSubModule(name = "Min Pitch Speed", parent = "Rotation Mode", modeParentString = {"Augustus", "Simple"}, min = 1, max = 180, increment = 1.0)
    public static float minPitchSpeed = 30;

    @RegisterSubModule(name = "Max Pitch Speed", parent = "Rotation Mode", modeParentString = {"Augustus", "Simple"}, min = 1, max = 180, increment = 1.0)
    public static float maxPitchSpeed = 50;

    @RegisterSubModule(name = "Rotation Smoothing", parent = "Rotation Mode", modeParentString = "Smooth", min = 1, max = 50)
    public static float smoothRotationSpeed = 1;

    @RegisterSubModule(name = "Easing", parent = "Rotation Mode", modeParentString = "Eased")
    public static EasingUtil.EasingFunctions easingFunction = EasingUtil.EasingFunctions.Ease_In_Out_Sine;

    @RegisterSubModule(name = "Easing Ticks", parent = "Rotation Mode", modeParentString = "Eased", min = 3, max = 20)
    public static int easingTicks = 10;

    @RegisterSubModule(name = "Client Side", parent = "Rotation")
    public static boolean clientSideRotation = true;

    @RegisterSubModule(name = "Randomize Rotations", parent = "Rotation")
    public static boolean randomizeRotations = true;

    @RegisterSubModule(name = "Jitter Pitch", parent = "Rotation", description = "Pitch Moves Up And Down")
    public static boolean jitterPitch = false;
    @RegisterSubModule(name = "Jitter Ticks", parent = "Jitter Pitch", min = 2, max = 500)
    public static int pitchJitter = 20;
    @RegisterSubModule(name = "Jitter Size", parent = "Jitter Pitch", min = 0.1, max = 1.5)
    public static double jitterSize = 0.3;

    // ─── Attacking & Click Patterns ───────────────────────────────────────────
    @RegisterSubModule(name = "Attacking")
    public SubCategory attackingSubCat = new SubCategory();

    @RegisterSubModule(name = "Click Mode", parent = "Attacking")
    public static ClickMode clickMode = ClickMode.Normal;
    public enum ClickMode {
        Normal, Delay, HurtTime, Gaussian, Advanced
    }

    @RegisterSubModule(name = "CPS Min", min = 1, max = 40, increment = 1, parent = "Click Mode", modeParentString = "Normal")
    public static double cpsMin = 9;
    @RegisterSubModule(name = "CPS Max", min = 1, max = 40, increment = 1, parent = "Click Mode", modeParentString = "Normal")
    public static double cpsMax = 13;

    @RegisterSubModule(name = "Attack Delay Min", min = 10, max = 1000, increment = 10, parent = "Click Mode", modeParentString = "Delay")
    public static double minAttackDelay = 60;
    @RegisterSubModule(name = "Attack Delay Max", min = 10, max = 1000, increment = 10, parent = "Click Mode", modeParentString = "Delay")
    public static double maxAttackDelay = 100;

    @RegisterSubModule(name = "Gaussian Value", min = 0.5, max = 6.0, increment = 0.1, parent = "Click Mode", modeParentString = "Gaussian")
    public static double gFunctionVal = 4.0;

    @RegisterSubModule(name = "Hit Chance", parent = "Attacking", min = 1, max = 100, increment = 1.0)
    public static double hitChance = 100.0;

    @RegisterSubModule(name = "Swing Misses", parent = "Attacking", description = "Swings even if out of raytrace range")
    public static boolean swingMisses = true;

    @RegisterSubModule(name = "Swords Only", parent = "Attacking")
    public static boolean swordOnlyAura = true;

    @RegisterSubModule(name = "Left Click Only", parent = "Attacking", description = "Only enables when left click is held down")
    public static boolean leftClickDownOnly = false;

    @RegisterSubModule(name = "Not While Mining", parent = "Attacking", description = "Only enables when not mining a block")
    public static boolean noMine = true;

    @RegisterSubModule(name = "No GUI", parent = "Attacking", description = "Disables aura while in any gui")
    public static boolean noGUI = true;

    // ─── Auto Block ───────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Auto Block")
    public SubCategory autoBlockSubCat = new SubCategory();

    @RegisterSubModule(name = "Autoblock Mode", parent = "Auto Block", description = "Combat autoblock mode")
    public static AutoBlockMode autoblockMode = AutoBlockMode.Watchdog;
    public enum AutoBlockMode {
        None, Basic, Watchdog, Intave, BlocksMC, Verus, Fake
    }

    @RegisterSubModule(name = "Auto Block Range", parent = "Auto Block", max = 6)
    public static double autoblockRange = 3.5;

    @RegisterSubModule(name = "AB Through Walls", parent = "Auto Block")
    public static boolean autoblockThroughWalls = true;

    @RegisterSubModule(name = "Packet Block", parent = "Auto Block")
    public static boolean packetBlock = false;

    @RegisterSubModule(name = "Watchdog Mode", parent = "Auto Block", modeParentString = "Watchdog")
    public static WatchdogLogicMode watchdogLogic = WatchdogLogicMode.Enemy_IFrame;
    public enum WatchdogLogicMode {
        Enemy_IFrame, Smart, Player_IFrame
    }

    @RegisterSubModule(name = "APS", min = 1.0, max = 20.0, increment = 1.0, parent = "Auto Block", modeParentString = "Watchdog")
    public static double autoblockAPS = 15.0;

    // ─── Runtime State ────────────────────────────────────────────────────────
    private static long nextAttackTime = 0;
    private static int easedRotationTick = 1;
    public static EntityLivingBase lastTarget = null;
    private static int switchTargetIndex = 0;
    private static RotationUtil.Rotation lastRotation = null;
    private static long lastAutoblockCycleTime = 0;
    private static boolean inAutoblockBurst = false;
    private static long lastTargetSwitchTime = 0;
    private static double dynamicRange = 3.1;
    private static long lastHazeHitTime = 0;

    @Getter private static boolean isBlocking = false, isServerBlocking = false;
    public static boolean swingQueued = false;
    public static boolean clickBlockQueued = false;
    private static ItemStack itemInUse = null;
    private static AutoBlockMode lastAutoblockMode;

    // ─── Rotation Event (Step 2: Rotate) ──────────────────────────────────────
    @SubscribeEvent(priority = 999)
    public static void onRotationEvent(RotationEvent event) {
        if (!shouldRotate()) {
            lastRotation = null;
            easedRotationTick = 0;
            return;
        }

        EntityLivingBase prevTarget = lastTarget;
        EntityLivingBase target = getTarget();

        if (target == null || !shouldRotateToEntity(target)) {
            lastRotation = null;
            easedRotationTick = 0;
            return;
        }

        if (target != prevTarget) easedRotationTick = 0;

        lastRotation = calculateRotation(target);
        event.rotation = new RotationUtil.Rotation(lastRotation.pitch, lastRotation.yaw);
    }

    // ─── Player Update & Attack Event (Step 1: Locate Target, Step 3: Attack) ──
    @SubscribeEvent
    public static void tryAttackTarget(PlayerUpdateEvent event) {
        updateDynamicRange();

        Entity target = WorldUtil.getMouseOver(PlayerUtil.currentRotation(), dynamicRange, throughWalls);

        if (target == lastTarget) easedRotationTick -= 2;
        if (!shouldAttack()) return;

        if (rotations == KillAuraRotations.None) {
            EntityLivingBase bestTarget = getTarget();
            if (bestTarget != null && shouldAttackEntity(bestTarget)) executeAttack(bestTarget);
            return;
        }

        if (!isValidTargetEntity(target, false)) {
            if (swingMisses) PlayerUtil.swingHand();
            return;
        }

        executeAttack(target);
    }

    @SubscribeEvent
    public static void onPlayerMotion(MotionEvent event) {
        if (lastTarget != null && lastRotation != null && clientSideRotation
                && rotations != KillAuraRotations.Snap && shouldRotate()
                && shouldRotateToEntity(lastTarget)) {
            C.p().rotationYaw = RotationUtil.applyWrap360(C.p().rotationYaw, lastRotation.yaw);
            C.p().rotationPitch = lastRotation.pitch;
        }
    }

    @SubscribeEvent
    public static void clickMouseEvent(ClickMouseEvent.Left event) {
        if (willSwing()) {
            event.setCancelled(true);
        }
    }

    // ─── Auto Block Loop ──────────────────────────────────────────────────────
    @SubscribeEvent(priority = 998)
    public static void tickAutoBlock(PlayerUpdateEvent event) {
        if (Fucker.getCurrentTarget() != null && Fucker.noAutoblock) {
            stopBlocking();
            return;
        }
        if (C.p().getHeldItem() != itemInUse || autoblockMode != lastAutoblockMode) {
            stopBlocking();
        }

        lastAutoblockMode = autoblockMode;

        if (autoblockMode == AutoBlockMode.None) {
            stopBlocking();
            return;
        }

        List<EntityLivingBase> targets = getPossibleTargets(autoblockRange, autoblockThroughWalls);

        if (targets.isEmpty() || lastTarget == null) {
            stopBlocking();
            return;
        }

        if (C.p().getHeldItem() != null && C.p().getHeldItem().getItemUseAction() == EnumAction.BLOCK) {
            tickBlocking();
        }
    }

    @SubscribeEvent(priority = 999)
    public static void tickSwingQueued(PlayerUpdateEvent event) {
        if (PlayerUtil.canAttack() && swingQueued) {
            MinecraftBridge.from(C.mc).bridge$clickMouse();
            swingQueued = false;
        }

        if (PlayerUtil.canAttack() && clickBlockQueued) {
            MinecraftBridge.from(C.mc).bridge$sendClickBlockToController(
                    C.mc.currentScreen == null && C.mc.gameSettings.keyBindAttack.isKeyDown() && C.mc.inGameHasFocus);
            clickBlockQueued = false;
        }
    }

    public static boolean isBlockingSwing() {
        return PlayerUtil.isUsingItem() || PlayerUtil.getLastUnblock() == MovementUtil.ticks;
    }

    public static void stopBlocking() {
        setBlocking(false, false);
    }

    public static void tickBlocking() {
        if (autoblockMode == AutoBlockMode.Watchdog) {
            List<EntityLivingBase> targets = getPossibleTargets(autoblockRange, autoblockThroughWalls);
            if (targets.isEmpty()) {
                stopBlocking();
                return;
            }
            targets.sort(Comparator.comparingDouble(KillAura::getDistanceToEntity));
            EntityLivingBase target = targets.get(0);

            if (watchdogLogic == WatchdogLogicMode.Enemy_IFrame || watchdogLogic == WatchdogLogicMode.Smart) {
                long now = System.currentTimeMillis();
                double targetCycleMs = 1000.0 / Math.max(1.0, Math.min(20.0, autoblockAPS));

                boolean enemyInIFrame = target.hurtResistantTime > 10 || target.hurtTime > 0;
                boolean playerInIFrame = watchdogLogic == WatchdogLogicMode.Smart && (C.p().hurtTime > 0 || C.p().hurtResistantTime > 10);

                if (enemyInIFrame || playerInIFrame) {
                    setBlocking(true, true);
                    inAutoblockBurst = false;
                } else {
                    boolean timePassed = (now - lastAutoblockCycleTime) >= targetCycleMs;
                    if (timePassed) {
                        lastAutoblockCycleTime = now;
                        inAutoblockBurst = true;
                        stopBlocking();
                    } else if (inAutoblockBurst) {
                        if (now - lastAutoblockCycleTime >= (targetCycleMs / 2.0)) {
                            inAutoblockBurst = false;
                            setBlocking(true, true);
                        } else {
                            stopBlocking();
                        }
                    } else {
                        setBlocking(true, true);
                    }
                }
            } else {
                boolean playerIFrame = C.p().hurtTime > 0 || C.p().hurtResistantTime > 10;
                if (playerIFrame) {
                    setBlocking(true, true);
                } else {
                    stopBlocking();
                }
            }
        } else if (autoblockMode == AutoBlockMode.BlocksMC || autoblockMode == AutoBlockMode.Intave) {
            setBlocking(true, false);
        } else {
            setBlocking(true, autoblockMode == AutoBlockMode.Basic || autoblockMode == AutoBlockMode.Verus);
        }
    }

    private static boolean setBlocking(boolean clientSide, boolean serverSide) {
        if (serverSide != isServerBlocking) {
            boolean blockSuccess = tryBlock(serverSide);
            if (!blockSuccess) return false;
            isServerBlocking = serverSide;
        }

        isBlocking = clientSide;
        itemInUse = C.p().getHeldItem();

        return true;
    }

    public static boolean canSwingWhileBlocking() {
        return (autoblockMode == AutoBlockMode.Basic || autoblockMode == AutoBlockMode.BlocksMC) && ModuleManager.isEnabled(KillAura.class) && isServerBlocking;
    }

    public static boolean tryBlock(boolean down) {
        if (down && MovementUtil.getOverriddenKeybinds().containsKey(C.mc.gameSettings.keyBindUseItem)
                && !MovementUtil.getOverriddenKeybinds().get(C.mc.gameSettings.keyBindUseItem))
            return false;

        if (packetBlock) {
            if (down) PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(C.p().getHeldItem()));
            else PacketUtil.sendPacket(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        } else {
            KeyBindingBridge.from(C.mc.gameSettings.keyBindUseItem).bridge$setDown(down);
            if (down) MinecraftBridge.from(C.mc).bridge$rightClickMouse();
            else C.mc.playerController.onStoppedUsingItem(C.p());
        }

        return true;
    }

    // ─── Targeting & Locating Algorithms ──────────────────────────────────────
    private static void updateDynamicRange() {
        if (hazeRange && lastTarget != null && lastTarget.hurtTime == 10) {
            dynamicRange = Math.min(dynamicRange + hazeAdd, hazeMax);
            lastHazeHitTime = System.currentTimeMillis();
        } else if (!hazeRange || Math.abs(System.currentTimeMillis() - lastHazeHitTime) > 1000L) {
            dynamicRange = killAuraAttackRange;
        }
    }

    private static boolean willSwing() {
        if (!shouldAttack()) return false;
        if (rotations == KillAuraRotations.None) return true;

        if (!isValidTargetEntity(WorldUtil.getMouseOver(PlayerUtil.currentRotation(), dynamicRange, throughWalls), false)) {
            return swingMisses;
        }

        return true;
    }

    private static boolean shouldAura() {
        return (!swordOnlyAura || InventoryUtil.getHeldItem() instanceof ItemSword)
                && (Fucker.getCurrentTarget() == null || !Fucker.noKillAura)
                && (!leftClickDownOnly || Mouse.isButtonDown(0))
                && (!noGUI || C.mc.currentScreen == null)
                && (!noMine || PlayerControllerMPBridge.from(C.mc.playerController).bridge$getCurBlockDamageMP() == 0);
    }

    private static boolean shouldAttack() {
        return lastTarget != null
                && shouldAura()
                && System.currentTimeMillis() >= nextAttackTime
                && PlayerUtil.canAttack()
                && !Fucker.shouldRotate()
                && (isTargetInFOV(lastTarget) || isValidTargetEntity(
                        WorldUtil.getMouseOver(PlayerUtil.currentRotation(), dynamicRange, throughWalls), false));
    }

    private static List<EntityLivingBase> getPossibleTargets(double reach, boolean walls) {
        if (C.w() == null) return new ArrayList<>();
        return C.w().getEntities(EntityLivingBase.class, entity -> canTargetEntity(entity, reach, walls));
    }

    private static boolean canTargetEntity(EntityLivingBase entity, double range, boolean walls) {
        if (!isValidTargetEntity(entity, false)) return false;
        double distance = getDistanceToEntity(entity);
        if (distance > range) return false;
        if (!walls && !C.p().canEntityBeSeen(entity) && !smartAim) return false;
        return true;
    }

    public static boolean isValidTargetEntity(Entity entity, boolean visual) {
        if (entity == null || entity == C.p() || entity.isDead) return false;
        if (!(entity instanceof EntityLivingBase)) return false;
        EntityLivingBase base = (EntityLivingBase) entity;
        if (base.deathTime > 1 || base.ticksExisted < 1) return false;
        if (base instanceof EntitySlime) return false;

        if (base.isInvisible() && !targetInvisibles) return false;
        if (base instanceof EntityArmorStand && !targetArmorStands) return false;
        if (base instanceof EntityAnimal && !targetAnimals) return false;
        if (base instanceof EntityVillager && !targetVillagers) return false;
        if (base instanceof EntityMob && !targetMobs) return false;
        if (base instanceof EntityPlayer && !targetPlayers) return false;

        if (TargetUtil.isBot(entity)) return false;
        if (TargetUtil.isTeam(entity, visual)) return false;

        return true;
    }

    public static double getDistanceToEntity(EntityLivingBase entity) {
        return C.p().getPositionEyes(1).distanceTo(getBestTargetVec(entity));
    }

    public static Vec3 getBestTargetVec(EntityLivingBase entity) {
        if (bestHitVec) {
            Vec3 positionEyes = C.p().getPositionEyes(1.0F);
            float border = entity.getCollisionBorderSize();
            AxisAlignedBB bb = entity.getEntityBoundingBox().expand(border, border, border);
            double ex = MathHelper.clamp_double(positionEyes.xCoord, bb.minX, bb.maxX);
            double ey = MathHelper.clamp_double(positionEyes.yCoord, bb.minY, bb.maxY);
            double ez = MathHelper.clamp_double(positionEyes.zCoord, bb.minZ, bb.maxZ);
            return new Vec3(ex, ey, ez);
        }
        return new Vec3(entity.posX, entity.posY + (double) entity.getEyeHeight() / 2.0, entity.posZ);
    }

    private static EntityLivingBase getTarget() {
        double scanRange = Math.max(preRange, dynamicRange);
        List<EntityLivingBase> sortedTargets = getPossibleTargets(scanRange, throughWalls)
                .stream()
                .sorted(Comparator.comparingDouble(KillAura::getDistanceToEntity))
                .sorted(Comparator.comparingDouble(entity -> {
                    switch (killAuraSorting) {
                        case Distance:
                            return getDistanceToEntity(entity);
                        case Health:
                            return entity.getHealth();
                        case Hurt_Time:
                            return entity.hurtTime;
                        case FOV:
                            return calculateFOVAngle(entity);
                        case Best:
                            return getDistanceToEntity(entity) * 2.0 + entity.getHealth() + entity.hurtTime * 4.0;
                        case UltimateSwitch:
                            return entity.hurtTime * 6.0 + getDistanceToEntity(entity);
                        default:
                            return 0;
                    }
                })).collect(Collectors.toList());

        if (sortedTargets.isEmpty()) {
            lastTarget = null;
            return null;
        }

        int targetIndex = 0;
        switch (killAuraTarget) {
            case Single:
                targetIndex = (lastTarget != null && sortedTargets.contains(lastTarget) && shouldAttackEntity(lastTarget))
                        ? sortedTargets.indexOf(lastTarget) : 0;
                break;
            case Switch:
                long now = System.currentTimeMillis();
                if (now - lastTargetSwitchTime >= targetDelay) {
                    switchTargetIndex = (switchTargetIndex + 1) % sortedTargets.size();
                    lastTargetSwitchTime = now;
                }
                targetIndex = switchTargetIndex % sortedTargets.size();
                break;
            case Best:
            default:
                targetIndex = 0;
                break;
        }

        lastTarget = sortedTargets.get(targetIndex);
        return lastTarget;
    }

    private static double calculateFOVAngle(EntityLivingBase entity) {
        Vec3 eyes = C.p().getPositionEyes(1.0F);
        Vec3 targetVec = getBestTargetVec(entity);
        RotationUtil.Rotation rot = RotationUtil.getRotation(RotationUtil.getCurrentClientRotation(), eyes, targetVec);
        float yawDiff = Math.abs(RotationUtil.applyWrap360(C.p().rotationYaw, rot.yaw) - C.p().rotationYaw);
        return yawDiff;
    }

    private static boolean shouldAttackEntity(EntityLivingBase entity) {
        return getDistanceToEntity(entity) <= dynamicRange;
    }

    private static boolean shouldRotate() {
        return shouldAura()
                && (rotations != KillAuraRotations.Snap || shouldAttack())
                && rotations != KillAuraRotations.None;
    }

    private static boolean shouldRotateToEntity(EntityLivingBase entity) {
        return getDistanceToEntity(entity) <= preRange && isTargetInFOV(entity);
    }

    private static boolean isTargetInFOV(EntityLivingBase entity) {
        if (FOV >= 360) return true;
        Vec3 targetPoint = getBestTargetVec(entity);
        RotationUtil.Rotation rot = RotationUtil.getRotation(RotationUtil.getCurrentClientRotation(), C.p().getPositionEyes(1.0F), targetPoint);
        float yawDiff = Math.abs(RotationUtil.applyWrap360(RotationUtil.getCurrentClientRotation().yaw, rot.yaw) - RotationUtil.getCurrentClientRotation().yaw);
        return yawDiff <= (FOV / 2.0);
    }

    // ─── Rotation Calculation (Step 2) ────────────────────────────────────────
    private static RotationUtil.Rotation calculateRotation(EntityLivingBase entity) {
        Vec3 targetPoint = getBestTargetVec(entity);

        if (jitterPitch) {
            double extraYcoord = EasingUtil.EasingFunctions.Ease_In_Out_Sine.ease(
                    (MovementUtil.ticks % pitchJitter) / (pitchJitter / 2d)) * jitterSize;
            targetPoint = targetPoint.addVector(0,
                    targetPoint.yCoord >= entity.getEntityBoundingBox().maxY - entity.height / 2
                            ? -extraYcoord : extraYcoord, 0);
        }

        RotationUtil.Rotation current = clientSideRotation ? RotationUtil.getCurrentClientRotation() : PlayerUtil.lastRotation();
        RotationUtil.Rotation targetRotation = RotationUtil.getRotation(current, C.p().getPositionEyes(1.0F), targetPoint);

        switch (rotations) {
            case Augustus: {
                SecureRandom secRandom = new SecureRandom();
                float deltaYaw = (float) (MathUtil.getRandomInRange(minYawSpeed, maxYawSpeed) / 2.0f + secRandom.nextFloat());
                float deltaPitch = (float) (MathUtil.getRandomInRange(minPitchSpeed, maxPitchSpeed) / 2.0f + secRandom.nextFloat());

                if (randomizeRotations) {
                    deltaYaw += (float) (ThreadLocalRandom.current().nextDouble(-1.0, 1.0));
                    deltaPitch += (float) (ThreadLocalRandom.current().nextDouble(-1.0, 1.0));
                }

                return RotationUtil.getLimitedRotation(current, targetRotation, Math.max(deltaYaw, deltaPitch));
            }
            case Simple:
                return RotationUtil.getLimitedRotation(current, targetRotation, (float) MathUtil.getRandomInRange(minYawSpeed, maxYawSpeed));
            case Smooth:
                return RotationUtil.getSmoothRotation(current, targetRotation, smoothRotationSpeed);
            case Eased:
                if (easedRotationTick < 0) easedRotationTick = 1;
                if (easedRotationTick < easingTicks) easedRotationTick++;
                return RotationUtil.getEasedRotation(current, targetRotation, easingFunction, (double) easedRotationTick / easingTicks);
            default:
                return RotationUtil.applyGcd(current, targetRotation);
        }
    }

    // ─── Attack Execution (Step 3) ────────────────────────────────────
    private static void executeAttack(Entity target) {
        if (hitChance < 100.0) {
            int roll = ThreadLocalRandom.current().nextInt(0, 100);
            if (roll > hitChance) return;
        }

        if (PlayerUtil.attack(target)) {
            long delay = computeClickDelay();
            nextAttackTime = System.currentTimeMillis() + delay;
            switchTargetIndex += 1;
        }
    }

    private static long computeClickDelay() {
        switch (clickMode) {
            case Delay:
                return (long) MathUtil.getRandomInRange(minAttackDelay, maxAttackDelay);
            case HurtTime:
                if (lastTarget != null && lastTarget.hurtTime > 0) {
                    return 250L + ThreadLocalRandom.current().nextInt(50);
                }
                return (long) (1000.0 / MathUtil.getRandomInRange(cpsMin, cpsMax));
            case Gaussian: {
                double mean = (cpsMin + cpsMax) / 2.0;
                double deviation = gFunctionVal;
                double randomizedCps = Math.max(1.0, (new Random().nextGaussian() * deviation) + mean);
                return (long) (1000.0 / randomizedCps);
            }
            case Advanced: {
                if (C.p().hurtTime > 0) {
                    return (long) (1000.0 / MathUtil.getRandomInRange(12.0, 16.0));
                }
                return (long) (1000.0 / MathUtil.getRandomInRange(cpsMin, cpsMax));
            }
            case Normal:
            default: {
                double randomCps = MathUtil.getRandomInRange(cpsMin, cpsMax);
                return (long) (1000.0 / Math.max(1.0, randomCps));
            }
        }
    }

    @Override
    protected void onEnable() {
        dynamicRange = killAuraAttackRange;
        nextAttackTime = 0;
        lastTarget = null;
    }

    @Override
    protected void onDisable() {
        stopBlocking();
        lastTarget = null;
        lastRotation = null;
    }

    @Override
    public String arrayListExtraInfo() {
        return mode.name() + " | " + killAuraSorting.name();
    }
}


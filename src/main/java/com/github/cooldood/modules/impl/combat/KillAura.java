package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.*;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.AntiBotUtil;
import com.github.cooldood.utils.minecraft.PacketUtil;
import com.github.cooldood.utils.minecraft.RotationUtil;
import com.github.cooldood.utils.minecraft.WorldUtil;
import lombok.Getter;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KillAura - Ported from OpenMyau-Plus.
 * Features:
 *  - Modes: Single, Switch
 *  - Sorting: Distance, Health, HurtTime, FOV
 *  - CPS: Normal randomized, Record (real human click pattern array)
 *  - Rotations: None, Legit, Silent, LockView, LiquidBounce, Hypixel (Raven BS)
 *  - Multi-point LiquidBounce body scan, predictive lead, and angle limiting
 *  - AutoBlock: None, Vanilla, Spoof, Hypixel, Interact, Swap, Legit, Fake
 *  - Movement Correction: None, Silent, Strict
 *  - AntiBot & Team filtering
 */
@RegisterModule(
        name = "Kill Aura",
        description = "KillAura ported from OpenMyau-Plus with LiquidBounce and Hypixel rotations.",
        category = Category.COMBAT
)
public class KillAura extends Module {

    // ── Target & Combat Mode Settings ───────────────────────────────────────
    public enum Mode { Single, Switch }
    @RegisterSubModule(name = "Mode")
    public static Mode mode = Mode.Switch;

    public enum SortMode { Distance, Health, HurtTime, FOV }
    @RegisterSubModule(name = "Sort")
    public static SortMode sort = SortMode.Distance;

    public enum CPSMode { Normal, Record }
    @RegisterSubModule(name = "CPS Mode")
    public static CPSMode cpsMode = CPSMode.Normal;

    @RegisterSubModule(name = "Min CPS", min = 1.0, max = 20.0, increment = 1.0)
    public static double minCPS = 12.0;

    @RegisterSubModule(name = "Max CPS", min = 1.0, max = 20.0, increment = 1.0)
    public static double maxCPS = 15.0;

    @RegisterSubModule(name = "Attack Range", min = 3.0, max = 6.0, increment = 0.1)
    public static double attackRange = 3.8;

    @RegisterSubModule(name = "Swing Range", min = 3.0, max = 6.0, increment = 0.1)
    public static double swingRange = 4.2;

    @RegisterSubModule(name = "FOV", min = 30.0, max = 360.0, increment = 10.0)
    public static double fov = 360.0;

    @RegisterSubModule(name = "Switch Delay", min = 0.0, max = 1000.0, increment = 25.0)
    public static double switchDelay = 150.0;

    // ── AutoBlock Settings ──────────────────────────────────────────────────
    public enum AutoBlockMode { NONE, VANILLA, SPOOF, HYPIXEL, INTERACT, SWAP, LEGIT, FAKE }
    @RegisterSubModule(name = "Auto Block")
    public static AutoBlockMode autoBlock = AutoBlockMode.HYPIXEL;

    @RegisterSubModule(name = "AutoBlock Range", min = 3.0, max = 8.0, increment = 0.1)
    public static double autoBlockRange = 5.0;

    @RegisterSubModule(name = "AutoBlock CPS", min = 1.0, max = 12.0, increment = 1.0)
    public static double autoBlockCPS = 8.0;

    @RegisterSubModule(name = "AutoBlock Require Press")
    public static boolean autoBlockRequirePress = false;

    // ── Rotation Modes ──────────────────────────────────────────────────────
    public enum RotationType { NONE, Legit, Silent, LockView, LiquidBounce, Hypixel }
    @RegisterSubModule(name = "Rotations")
    public static RotationType rotations = RotationType.LiquidBounce;

    public enum MoveFixMode { NONE, Silent, Strict }
    @RegisterSubModule(name = "Move Fix")
    public static MoveFixMode moveFix = MoveFixMode.Silent;

    @RegisterSubModule(name = "Smooth Back")
    public static boolean smoothBack = true;

    @RegisterSubModule(name = "Angle Step", min = 30.0, max = 180.0, increment = 5.0)
    public static double angleStep = 90.0;

    @RegisterSubModule(name = "Smoothing", min = 0.0, max = 100.0, increment = 5.0)
    public static double smoothing = 0.0;

    // ── LiquidBounce Rotation Engine SubSettings ────────────────────────────
    @RegisterSubModule(name = "LB-HSpeed", min = 1.0, max = 180.0, increment = 5.0)
    public static double liquidBounceHorizontalSpeed = 180.0;

    @RegisterSubModule(name = "LB-VSpeed", min = 1.0, max = 180.0, increment = 5.0)
    public static double liquidBounceVerticalSpeed = 180.0;

    @RegisterSubModule(name = "LB-Smooth", min = 0.1, max = 1.0, increment = 0.05)
    public static double liquidBounceSmoothFactor = 0.6;

    @RegisterSubModule(name = "LB-Predict")
    public static boolean liquidBouncePredict = true;

    @RegisterSubModule(name = "LB-Predict Size", min = 0.0, max = 3.0, increment = 0.1, parent = "LB-Predict")
    public static double liquidBouncePredictSize = 1.0;

    @RegisterSubModule(name = "LB-Randomize")
    public static boolean liquidBounceRandomize = true;

    @RegisterSubModule(name = "LB-Random Range", min = 0.0, max = 1.0, increment = 0.05, parent = "LB-Randomize")
    public static double liquidBounceRandomizeRange = 0.4;

    @RegisterSubModule(name = "LB-Body Min", min = 0.0, max = 1.0, increment = 0.05)
    public static double liquidBounceBodyPointMin = 0.2;

    @RegisterSubModule(name = "LB-Body Max", min = 0.0, max = 1.0, increment = 0.05)
    public static double liquidBounceBodyPointMax = 0.85;

    // ── Hypixel / Raven Rotation SubSettings ────────────────────────────────
    @RegisterSubModule(name = "Hypixel Smoothing", min = 0.0, max = 10.0, increment = 1.0)
    public static double ravenSmoothing = 2.0;

    @RegisterSubModule(name = "Hypixel Predict", min = 0.0, max = 5.0, increment = 1.0)
    public static double ravenPredictTicks = 1.0;

    @RegisterSubModule(name = "Hypixel Yaw Random", min = 0.0, max = 5.0, increment = 1.0)
    public static double ravenYawRandom = 1.0;

    // ── Target Filter Settings ──────────────────────────────────────────────
    @RegisterSubModule(name = "Players")
    public static boolean players = true;

    @RegisterSubModule(name = "Mobs")
    public static boolean mobs = false;

    @RegisterSubModule(name = "Animals")
    public static boolean animals = false;

    @RegisterSubModule(name = "Bosses")
    public static boolean bosses = false;

    @RegisterSubModule(name = "Golems")
    public static boolean golems = false;

    @RegisterSubModule(name = "Silverfish")
    public static boolean silverfish = false;

    @RegisterSubModule(name = "Teams")
    public static boolean teams = true;

    @RegisterSubModule(name = "Bot Check")
    public static boolean botCheck = true;

    @RegisterSubModule(name = "Through Walls")
    public static boolean throughWalls = false;

    @RegisterSubModule(name = "Require Press")
    public static boolean requirePress = false;

    @RegisterSubModule(name = "Allow Mining")
    public static boolean allowMining = true;

    @RegisterSubModule(name = "Weapons Only")
    public static boolean weaponsOnly = true;

    @RegisterSubModule(name = "Allow Tools", parent = "Weapons Only")
    public static boolean allowTools = false;

    @RegisterSubModule(name = "Inventory Check")
    public static boolean inventoryCheck = true;

    // ── Internal State & Click Pattern ──────────────────────────────────────
    private static final int[] clickPattern = {
            16, 22, 14, 46, 18, 8, 8, 63, 25, 25, 12, 39, 26, 18, 6, 62, 26, 18, 21, 40,
            26, 8, 16, 46, 26, 20, 15, 50, 25, 10, 11, 43, 25, 11, 37, 39, 25, 12, 18, 54,
            25, 25, 15, 41, 27, 9, 1, 66, 26, 17, 21, 48, 27, 8, 6, 62, 28, 19, 13, 47,
            26, 7, 14, 53, 27, 16, 29, 38, 27, 8, 6, 60, 27, 22, 19, 45, 26, 10, 10, 62,
            25, 20, 28, 22, 26, 19, 11, 57, 26, 16, 32, 36, 26, 9, 9, 66, 27, 19, 27, 38
    };

    private static final Random random = new Random();
    @Getter public static AttackData target = null;
    public static EntityLivingBase targetEntity = null;
    private static RotationUtil.Rotation serverRotation = new RotationUtil.Rotation(0, 0);
    private static boolean isSmoothBacking = false;
    private static int patternIndex = 0;
    private static long lastAttackTime = 0L;
    private static long attackDelayMS = 0L;
    private static int blockTick = 0;
    private static boolean blockingState = false;
    private static boolean isBlocking = false;
    private static long lastRotationUpdateTime = 0L;
    private static long lastTargetSwitchTime = 0L;
    private static boolean initializedRotation = false;

    // ── Main Event Hooks ────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onRotation(RotationEvent event) {
        if (C.p() == null || C.w() == null) {
            target = null;
            targetEntity = null;
            return;
        }

        if (!initializedRotation) {
            serverRotation = new RotationUtil.Rotation(C.p().rotationPitch, C.p().rotationYaw);
            initializedRotation = true;
        }

        updateTarget();

        boolean inRange = target != null && isBoxInSwingRange(target.getBox());
        if (!inRange && target != null) {
            if (smoothBack) {
                isSmoothBacking = true;
            }
            target = null;
            targetEntity = null;
        }

        if (target != null && inRange) {
            isSmoothBacking = false;
            float targetYaw = serverRotation.yaw;
            float targetPitch = serverRotation.pitch;

            if (rotations == RotationType.LiquidBounce) {
                RotationUtil.Rotation nextRot = updateLiquidBounceRotation(serverRotation);
                float[] fixed = applyGcd(nextRot.yaw, nextRot.pitch, serverRotation.yaw, serverRotation.pitch);
                serverRotation = new RotationUtil.Rotation(fixed[1], fixed[0]);
                targetYaw = fixed[0];
                targetPitch = fixed[1];
            } else if (rotations == RotationType.Hypixel) {
                float[] raw = getRavenRotations(target.getEntity());
                float[] fixedRot = ravenFixRotation(raw[0], raw[1], serverRotation.yaw, serverRotation.pitch);
                float[] smoothed = getRavenRotationsSmoothed(fixedRot, serverRotation.yaw, serverRotation.pitch);
                float finalYaw = smoothed[0];
                float finalPitch = MathHelper.clamp_float(smoothed[1], -90.0F, 90.0F);
                serverRotation = new RotationUtil.Rotation(finalPitch, finalYaw);
                targetYaw = finalYaw;
                targetPitch = finalPitch;
            } else if (rotations == RotationType.Legit || rotations == RotationType.Silent || rotations == RotationType.LockView) {
                float[] aim = getRotationsToEntity(target.getEntity());
                float[] fixed = applyGcd(aim[0], aim[1], serverRotation.yaw, serverRotation.pitch);
                serverRotation = new RotationUtil.Rotation(fixed[1], fixed[0]);
                targetYaw = fixed[0];
                targetPitch = fixed[1];
            }

            // Sync visual and packets
            if (rotations != RotationType.NONE) {
                event.rotation = new RotationUtil.Rotation(targetPitch, targetYaw);
                if (rotations == RotationType.LockView) {
                    C.p().rotationYaw = targetYaw;
                    C.p().rotationPitch = targetPitch;
                } else {
                    C.p().rotationYawHead = targetYaw;
                    C.p().renderYawOffset = targetYaw;
                }
            }
        } else if (smoothBack && isSmoothBacking) {
            float playerYaw = C.p().rotationYaw;
            float playerPitch = C.p().rotationPitch;
            float diffYaw = MathHelper.wrapAngleTo180_float(playerYaw - serverRotation.yaw);
            float diffPitch = playerPitch - serverRotation.pitch;

            if (Math.abs(diffYaw) > 1.0F || Math.abs(diffPitch) > 1.0F) {
                RotationUtil.Rotation smoothRot = getSmoothBackRotation(serverRotation, new RotationUtil.Rotation(playerPitch, playerYaw));
                float[] fixed = applyGcd(smoothRot.yaw, smoothRot.pitch, serverRotation.yaw, serverRotation.pitch);
                serverRotation = new RotationUtil.Rotation(fixed[1], fixed[0]);
                event.rotation = new RotationUtil.Rotation(serverRotation.pitch, serverRotation.yaw);
                C.p().rotationYawHead = serverRotation.yaw;
                C.p().renderYawOffset = serverRotation.yaw;
            } else {
                serverRotation = new RotationUtil.Rotation(playerPitch, playerYaw);
                isSmoothBacking = false;
            }
        } else {
            serverRotation = new RotationUtil.Rotation(C.p().rotationPitch, C.p().rotationYaw);
        }
    }

    @SubscribeEvent
    public static void onMotion(MotionEvent event) {
        if (C.p() == null || C.w() == null) return;

        long now = System.currentTimeMillis();
        boolean canHit = target != null && canAttack();
        boolean block = canHit && canAutoBlock();

        if (block) {
            handleAutoBlockPre();
        } else {
            isBlocking = false;
            if (blockingState) stopBlock();
        }

        if (canHit && now - lastAttackTime >= attackDelayMS) {
            if (performAttack(serverRotation.yaw, serverRotation.pitch)) {
                lastAttackTime = now;
                attackDelayMS = getAttackDelay();
                if (autoBlock == AutoBlockMode.INTERACT) {
                    interactAttack(serverRotation.yaw, serverRotation.pitch);
                }
            }
        }

        if (block) {
            handleAutoBlockPost();
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputEvent event) {
        if (target == null && !isSmoothBacking) return;
        if (moveFix == MoveFixMode.NONE) return;

        float targetYaw = serverRotation.yaw;
        if (moveFix == MoveFixMode.Strict) {
            C.p().rotationYaw = targetYaw;
            C.p().rotationYawHead = targetYaw;
        } else if (moveFix == MoveFixMode.Silent) {
            fixMovementSilent(event, targetYaw);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (C.p() == null || C.w() == null) {
            target = null;
            targetEntity = null;
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldUnloadEvent event) {
        resetState();
    }

    @SubscribeEvent
    public static void onRespawn(RespawnEvent event) {
        resetState();
    }

    // ── Attack Logic ────────────────────────────────────────────────────────
    private static boolean performAttack(float yaw, float pitch) {
        if (target == null || C.p() == null) return false;

        if (rotations == RotationType.LiquidBounce) {
            MovingObjectPosition intercept = rayTrace(target.getBox(), yaw, pitch, attackRange);
            if (intercept == null && !throughWalls) {
                return false;
            }
        }

        C.p().swingItem();

        if (cpsMode == CPSMode.Record) {
            patternIndex = (patternIndex + 1) % clickPattern.length;
        }

        PacketUtil.sendPacket(new C02PacketUseEntity(target.getEntity(), Action.ATTACK));
        C.mc.playerController.attackEntity(C.p(), target.getEntity());
        return true;
    }

    private static long getAttackDelay() {
        if (isBlocking) {
            return (long) (1000.0 / Math.max(1.0, autoBlockCPS));
        }
        if (cpsMode == CPSMode.Record) {
            if (patternIndex >= clickPattern.length) patternIndex = 0;
            return clickPattern[patternIndex];
        }
        double min = Math.min(minCPS, maxCPS);
        double max = Math.max(minCPS, maxCPS);
        double cps = min + (random.nextDouble() * (max - min));
        return (long) (1000.0 / Math.max(1.0, cps));
    }

    private static boolean canAttack() {
        if (target == null || C.p() == null) return false;
        if (inventoryCheck && C.mc.currentScreen instanceof GuiContainer) return false;
        if (weaponsOnly && !isHoldingWeapon()) return false;
        if (requirePress && !C.mc.gameSettings.keyBindAttack.isKeyDown()) return false;
        return true;
    }

    private static boolean isHoldingWeapon() {
        if (C.p() == null) return false;
        ItemStack held = C.p().getHeldItem();
        if (held == null) return false;
        if (held.getItem() instanceof ItemSword) return true;
        return allowTools && (held.getItem() instanceof ItemTool);
    }

    // ── AutoBlock Core ──────────────────────────────────────────────────────
    private static boolean canAutoBlock() {
        if (C.p() == null || C.p().getHeldItem() == null) return false;
        if (!(C.p().getHeldItem().getItem() instanceof ItemSword)) return false;
        if (autoBlockRequirePress && !C.mc.gameSettings.keyBindUseItem.isKeyDown()) return false;
        return autoBlock != AutoBlockMode.NONE;
    }

    private static void handleAutoBlockPre() {
        if (target == null || !hasValidTarget()) {
            isBlocking = false;
            return;
        }

        switch (autoBlock) {
            case VANILLA:
            case SPOOF:
                startBlock(C.p().getHeldItem());
                isBlocking = true;
                break;
            case HYPIXEL:
                if (blockTick == 0) {
                    stopBlock();
                    blockTick = 1;
                }
                isBlocking = true;
                break;
            case SWAP:
                int cur = C.p().inventory.currentItem;
                int empty = findEmptySlot(cur);
                PacketUtil.sendPacket(new C09PacketHeldItemChange(empty));
                PacketUtil.sendPacket(new C09PacketHeldItemChange(cur));
                startBlock(C.p().getHeldItem());
                isBlocking = true;
                break;
            case FAKE:
                isBlocking = true;
                break;
            default:
                break;
        }
    }

    private static void handleAutoBlockPost() {
        if (target == null) return;
        if (autoBlock == AutoBlockMode.HYPIXEL && blockTick == 1) {
            startBlock(C.p().getHeldItem());
            blockTick = 0;
        }
    }

    private static void startBlock(ItemStack itemStack) {
        if (itemStack == null) return;
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(itemStack));
        C.p().setItemInUse(itemStack, itemStack.getMaxItemUseDuration());
        blockingState = true;
    }

    private static void stopBlock() {
        if (C.p() == null) return;
        PacketUtil.sendPacket(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        C.p().stopUsingItem();
        blockingState = false;
    }

    private static void interactAttack(float yaw, float pitch) {
        if (target == null || C.p() == null) return;
        MovingObjectPosition mop = rayTrace(target.getBox(), yaw, pitch, 8.0);
        if (mop != null) {
            PacketUtil.sendPacket(new C02PacketUseEntity(
                    target.getEntity(),
                    new Vec3(mop.hitVec.xCoord - target.getX(), mop.hitVec.yCoord - target.getY(), mop.hitVec.zCoord - target.getZ())
            ));
            PacketUtil.sendPacket(new C02PacketUseEntity(target.getEntity(), Action.INTERACT));
            startBlock(C.p().getHeldItem());
        }
    }

    private static int findEmptySlot(int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot && C.p().inventory.getStackInSlot(i) == null) return i;
        }
        return Math.floorMod(currentSlot - 1, 9);
    }

    // ── Target Selection & Sorting ──────────────────────────────────────────
    private static void updateTarget() {
        long now = System.currentTimeMillis();
        boolean needsNew = target == null
                || !isValidTarget(target.getEntity())
                || !isBoxInSwingRange(target.getBox())
                || (mode == Mode.Switch && now - lastTargetSwitchTime >= switchDelay);

        if (!needsNew) return;

        List<EntityLivingBase> candidates = new ArrayList<>();
        for (Object o : C.w().loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase living = (EntityLivingBase) o;
            if (isValidTarget(living) && isInBlockRange(living)) {
                candidates.add(living);
            }
        }

        if (candidates.isEmpty()) {
            if (target != null && smoothBack) isSmoothBacking = true;
            target = null;
            targetEntity = null;
            return;
        }

        candidates.sort((e1, e2) -> {
            switch (sort) {
                case Health:
                    return Float.compare(e1.getHealth(), e2.getHealth());
                case HurtTime:
                    return Integer.compare(e1.hurtResistantTime, e2.hurtResistantTime);
                case FOV:
                    return Float.compare(getAngleToEntity(e1), getAngleToEntity(e2));
                case Distance:
                default:
                    return Double.compare(C.p().getDistanceToEntity(e1), C.p().getDistanceToEntity(e2));
            }
        });

        EntityLivingBase chosen = candidates.get(0);
        target = new AttackData(chosen);
        targetEntity = chosen;
        lastTargetSwitchTime = now;
    }

    public static boolean isValidTarget(EntityLivingBase entity) {
        if (entity == null || entity == C.p() || C.w() == null) return false;
        if (entity.isDead || entity.getHealth() <= 0 || entity.deathTime > 0) return false;
        if (entity instanceof EntityArmorStand) return false;
        if (getAngleToEntity(entity) > (float) fov) return false;
        if (!throughWalls && !C.p().canEntityBeSeen(entity)) return false;

        if (entity instanceof EntityPlayer) {
            if (!players) return false;
            if (botCheck && AntiBot.isBot(entity)) return false;
            if (teams && AntiBotUtil.isTeam(entity, false)) return false;
            return true;
        }
        if (entity instanceof EntityDragon || entity instanceof EntityWither) return bosses;
        if (entity instanceof EntityIronGolem) return golems;
        if (entity instanceof EntitySilverfish) return silverfish;
        if (entity instanceof EntityMob || entity instanceof EntitySlime) return mobs;
        if (entity instanceof EntityAnimal || entity instanceof EntityBat || entity instanceof EntitySquid || entity instanceof EntityVillager) return animals;
        return false;
    }

    private static boolean hasValidTarget() {
        return target != null && isValidTarget(target.getEntity());
    }

    private static boolean isInBlockRange(EntityLivingBase entity) {
        return C.p() != null && C.p().getDistanceToEntity(entity) <= Math.max(attackRange, autoBlockRange);
    }

    private static boolean isBoxInSwingRange(AxisAlignedBB box) {
        return C.p() != null && C.p().getPositionEyes(1.0F).distanceTo(WorldUtil.getClosestPoint(box)) <= swingRange;
    }

    private static float getAngleToEntity(EntityLivingBase entity) {
        double diffX = entity.posX - C.p().posX;
        double diffZ = entity.posZ - C.p().posZ;
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX))) - 90.0F;
        return Math.abs(MathHelper.wrapAngleTo180_float(yaw - C.p().rotationYaw));
    }

    // ── LiquidBounce Rotation Engine ────────────────────────────────────────
    private static RotationUtil.Rotation updateLiquidBounceRotation(RotationUtil.Rotation current) {
        if (target == null || System.currentTimeMillis() - lastRotationUpdateTime < 50) return current;
        lastRotationUpdateTime = System.currentTimeMillis();

        EntityLivingBase ent = target.getEntity();
        AxisAlignedBB bb = ent.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
        Vec3 eyes = C.p().getPositionEyes(1.0F);

        Vec3 targetPoint = searchCenterPoint(bb, eyes);
        if (targetPoint == null) {
            targetPoint = new Vec3((bb.minX + bb.maxX) / 2.0, (bb.minY + bb.maxY) / 2.0, (bb.minZ + bb.maxZ) / 2.0);
        }

        if (liquidBouncePredict) {
            double pX = ent.posX + ent.motionX * liquidBouncePredictSize;
            double pY = ent.posY + ent.motionY * liquidBouncePredictSize * 0.5;
            double pZ = ent.posZ + ent.motionZ * liquidBouncePredictSize;
            targetPoint = targetPoint.addVector(pX - ent.posX, pY - ent.posY, pZ - ent.posZ);
        }

        double diffX = targetPoint.xCoord - eyes.xCoord;
        double diffY = targetPoint.yCoord - eyes.yCoord;
        double diffZ = targetPoint.zCoord - eyes.zCoord;
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

        float targetYaw = MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
        float targetPitch = MathHelper.wrapAngleTo180_float((float) -Math.toDegrees(Math.atan2(diffY, dist)));

        // Limit angle change per tick with smoothFactor
        float maxH = (float) liquidBounceHorizontalSpeed;
        float maxV = (float) liquidBounceVerticalSpeed;
        float smooth = (float) liquidBounceSmoothFactor;

        float yawDiff = MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(targetYaw - current.yaw), -maxH, maxH) * smooth;
        float pitchDiff = MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(targetPitch - current.pitch), -maxV, maxV) * smooth;

        return new RotationUtil.Rotation(
                MathHelper.clamp_float(current.pitch + pitchDiff, -90.0F, 90.0F),
                current.yaw + yawDiff
        );
    }

    private static Vec3 searchCenterPoint(AxisAlignedBB bb, Vec3 eyes) {
        Vec3 best = null;
        double bestScore = Double.MAX_VALUE;

        for (double x = 0.0; x <= 1.0; x += 0.33) {
            for (double y = liquidBounceBodyPointMin; y <= liquidBounceBodyPointMax; y += 0.25) {
                for (double z = 0.0; z <= 1.0; z += 0.33) {
                    Vec3 point = new Vec3(
                            bb.minX + (bb.maxX - bb.minX) * x,
                            bb.minY + (bb.maxY - bb.minY) * y,
                            bb.minZ + (bb.maxZ - bb.minZ) * z
                    );

                    double dist = eyes.distanceTo(point);
                    if (dist > Math.max(attackRange, swingRange)) continue;

                    boolean visible = throughWalls || C.w().rayTraceBlocks(eyes, point, false, true, false) == null;
                    if (!visible && !throughWalls) continue;

                    double score = dist + (visible ? 0.0 : 5.0);
                    if (liquidBounceRandomize) {
                        score += random.nextDouble() * liquidBounceRandomizeRange * 3.0;
                    }

                    if (score < bestScore) {
                        bestScore = score;
                        best = point;
                    }
                }
            }
        }
        return best;
    }

    // ── Hypixel / Raven BS Rotation Port ────────────────────────────────────
    private static float[] getRavenRotations(EntityLivingBase entity) {
        double posX = entity.posX;
        double posZ = entity.posZ;
        int ticks = (int) ravenPredictTicks;
        if (ticks > 0) {
            double dX = entity.posX - entity.lastTickPosX;
            double dZ = entity.posZ - entity.lastTickPosZ;
            posX += dX * ticks;
            posZ += dZ * ticks;
        }
        double deltaX = posX - C.p().posX;
        double deltaZ = posZ - C.p().posZ;
        double deltaY = entity.posY + entity.getEyeHeight() * 0.9 - (C.p().posY + C.p().getEyeHeight());
        float yaw = C.p().rotationYaw + MathHelper.wrapAngleTo180_float(
                (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F - C.p().rotationYaw));
        float pitch = MathHelper.clamp_float(C.p().rotationPitch + MathHelper.wrapAngleTo180_float(
                (float) (-Math.toDegrees(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ))) - C.p().rotationPitch)) + 3.0F, -90.0F, 90.0F);
        return new float[]{yaw, pitch};
    }

    private static float[] ravenFixRotation(float targetYaw, float targetPitch, float yaw, float pitch) {
        float n5 = targetYaw - yaw;
        float abs = Math.abs(n5);
        float n7 = targetPitch - pitch;
        float n8 = C.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        double n9 = n8 * n8 * n8 * 1.2;
        float n10 = (float) (Math.round((double) n5 / n9) * n9);
        float n11 = (float) (Math.round((double) n7 / n9) * n9);
        targetYaw = yaw + n10;
        targetPitch = pitch + n11;
        if (abs >= 1.0F) {
            int factor = (int) ravenYawRandom;
            if (factor != 0) {
                int n13 = factor * 100 + (random.nextInt(61) - 30);
                targetYaw += (random.nextInt(n13 * 2 + 1) - n13) / 100.0F;
            }
        }
        return new float[]{targetYaw, MathHelper.clamp_float(targetPitch, -90.0F, 90.0F)};
    }

    private static float[] getRavenRotationsSmoothed(float[] rotations, float serverYaw, float serverPitch) {
        float unwrappedYaw = serverYaw + MathHelper.wrapAngleTo180_float(rotations[0] - serverYaw);
        float deltaYaw = unwrappedYaw - serverYaw;
        float deltaPitch = rotations[1] - serverPitch;

        float yawSmoothing = (float) Math.max(1.0, ravenSmoothing);
        float pitchSmoothing = yawSmoothing;

        float strafe = C.p().moveStrafing;
        if (strafe < 0 && deltaYaw < 0 || strafe > 0 && deltaYaw > 0) {
            yawSmoothing = Math.max(1.0F, yawSmoothing / 2.0F);
        }

        serverYaw += deltaYaw / yawSmoothing;
        serverPitch += deltaPitch / pitchSmoothing;
        return new float[]{serverYaw, serverPitch};
    }

    // ── Common Rotation & Raytrace Utilities ─────────────────────────────────
    private static float[] getRotationsToEntity(EntityLivingBase entity) {
        double diffX = entity.posX - C.p().posX;
        double diffY = (entity.posY + entity.getEyeHeight() * 0.8) - (C.p().posY + C.p().getEyeHeight());
        double diffZ = entity.posZ - C.p().posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));
        return new float[]{MathHelper.wrapAngleTo180_float(yaw), MathHelper.clamp_float(pitch, -90.0F, 90.0F)};
    }

    private static float[] applyGcd(float targetYaw, float targetPitch, float prevYaw, float prevPitch) {
        float sens = C.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = sens * sens * sens * 1.2F;
        if (gcd <= 0.0F) gcd = 0.001F;

        float dYaw = MathHelper.wrapAngleTo180_float(targetYaw - prevYaw);
        float dPitch = targetPitch - prevPitch;

        int mX = Math.round(dYaw / gcd);
        int mY = Math.round(dPitch / gcd);

        return new float[]{
                prevYaw + (mX * gcd),
                MathHelper.clamp_float(prevPitch + (mY * gcd), -90.0F, 90.0F)
        };
    }

    private static RotationUtil.Rotation getSmoothBackRotation(RotationUtil.Rotation current, RotationUtil.Rotation target) {
        float yawDiff = MathHelper.wrapAngleTo180_float(target.yaw - current.yaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(target.pitch - current.pitch);
        float speed = Math.max(6.0F, Math.abs(yawDiff) * 0.3F);
        float yawStep = MathHelper.clamp_float(yawDiff, -speed, speed);
        float pitchStep = MathHelper.clamp_float(pitchDiff, -speed, speed);
        return new RotationUtil.Rotation(current.pitch + pitchStep, current.yaw + yawStep);
    }

    public static MovingObjectPosition rayTrace(AxisAlignedBB box, float yaw, float pitch, double reach) {
        Vec3 eyes = C.p().getPositionEyes(1.0F);
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        Vec3 look = new Vec3((double) (f1 * f2), (double) f3, (double) (f * f2));
        Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        return box.calculateIntercept(eyes, end);
    }

    private static void fixMovementSilent(MovementInputEvent event, float targetYaw) {
        float forward = event.movementInput.moveForward;
        float strafe = event.movementInput.moveStrafe;
        if (forward == 0.0F && strafe == 0.0F) return;

        float diffRad = (float) Math.toRadians(targetYaw - C.p().rotationYaw);
        float cos = MathHelper.cos(diffRad);
        float sin = MathHelper.sin(diffRad);

        float newForward = forward * cos + strafe * sin;
        float newStrafe = strafe * cos - forward * sin;

        float mult = event.movementInput.sneak ? 0.3F : 1.0F;
        event.movementInput.moveForward = MathHelper.clamp_float(Math.round(newForward), -1.0F, 1.0F) * mult;
        event.movementInput.moveStrafe = MathHelper.clamp_float(Math.round(newStrafe), -1.0F, 1.0F) * mult;
    }

    private static void resetState() {
        target = null;
        targetEntity = null;
        isBlocking = false;
        blockingState = false;
        isSmoothBacking = false;
        blockTick = 0;
        initializedRotation = false;
    }

    @Override
    protected void onEnable() {
        resetState();
        if (C.p() != null) {
            serverRotation = new RotationUtil.Rotation(C.p().rotationPitch, C.p().rotationYaw);
        }
    }

    @Override
    protected void onDisable() {
        resetState();
        if (C.p() != null && blockingState) {
            stopBlock();
        }
    }

    // ── Compatibility Getters ───────────────────────────────────────────────
    public static boolean isBlocking() {
        return isBlocking;
    }

    public static boolean isServerBlocking() {
        return blockingState;
    }

    public static boolean canSwingWhileBlocking() {
        return true;
    }

    public static boolean isBlockingSwing() {
        return isBlocking;
    }

    // ── Inner AttackData Structure ──────────────────────────────────────────
    public static class AttackData {
        private final EntityLivingBase entity;

        public AttackData(EntityLivingBase entity) {
            this.entity = entity;
        }

        public EntityLivingBase getEntity() {
            return entity;
        }

        public AxisAlignedBB getBox() {
            return entity.getEntityBoundingBox();
        }

        public double getX() {
            return entity.posX;
        }

        public double getY() {
            return entity.posY;
        }

        public double getZ() {
            return entity.posZ;
        }
    }
}

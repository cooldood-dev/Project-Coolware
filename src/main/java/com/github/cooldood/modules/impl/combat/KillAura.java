package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.ClickMouseEvent;
import com.github.cooldood.events.impl.MotionEvent;
import com.github.cooldood.events.impl.MovementInputEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.events.impl.RotationEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.AntiBotUtil;
import com.github.cooldood.utils.minecraft.PacketUtil;
import com.github.cooldood.utils.minecraft.RotationUtil;
import lombok.Getter;
import net.minecraft.entity.Entity;
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
 * KillAura - Advanced Combat & Humanized Rotation System.
 * Features:
 *  - Non-linear power curve smoothing & deceleration ((Δθ / 45)^0.4)
 *  - Motion & Velocity extrapolation (V2-Predict)
 *  - Organic harmonic micro-sway & Gaussian noise (V2-Randomize)
 *  - Configurable target height offset (V2-Height, default 0.75 upper chest/neck)
 *  - Exact Minecraft mouse sensitivity GCD quantization ((sens*0.6+0.2)^3 * 1.2)
 *  - Smooth return to crosshair on target loss (V2-SmoothBack)
 *  - Anti-cheat compatible Silent MoveFix
 */
@RegisterModule(
        name = "Kill Aura",
        description = "Advanced KillAura with humanized non-linear rotations, velocity prediction, and GCD quantization.",
        category = Category.COMBAT
)
public class KillAura extends Module {

    // ── Combat Range & CPS ──────────────────────────────────────────────────
    @RegisterSubModule(name = "Range", min = 3.0, max = 6.0, increment = 0.1)
    public static double range = 4.2;

    @RegisterSubModule(name = "Block Range", min = 3.0, max = 8.0, increment = 0.1)
    public static double blockRange = 5.5;

    @RegisterSubModule(name = "CPS", min = 1.0, max = 20.0, increment = 1.0)
    public static double cps = 12.0;

    @RegisterSubModule(name = "Auto Block")
    public static boolean autoBlock = true;

    @RegisterSubModule(name = "Keep Sprint")
    public static boolean keepSprint = true;

    @RegisterSubModule(name = "Through Walls")
    public static boolean throughWalls = false;

    // ── Target Filter ───────────────────────────────────────────────────────
    @RegisterSubModule(name = "Target Players")
    public static boolean targetPlayers = true;

    @RegisterSubModule(name = "Target Mobs")
    public static boolean targetMobs = true;

    @RegisterSubModule(name = "Target Animals")
    public static boolean targetAnimals = false;

    @RegisterSubModule(name = "Target Invisibles")
    public static boolean targetInvisibles = false;

    // ── Rotation Core Settings ──────────────────────────────────────────────
    @RegisterSubModule(name = "Rotation Mode")
    public static RotationMode rotationMode = RotationMode.Silent;
    public enum RotationMode { Normal, Silent }

    @RegisterSubModule(name = "Move Fix")
    public static MoveFix moveFix = MoveFix.Silent;
    public enum MoveFix { Off, Strict, Silent, ChangeLook }

    @RegisterSubModule(name = "Rotation Speed", min = 2.0, max = 40.0, increment = 1.0)
    public static double rotationSpeed = 16.0;

    @RegisterSubModule(name = "Acceleration", min = 0.1, max = 1.0, increment = 0.05)
    public static double acceleration = 0.4;

    @RegisterSubModule(name = "GCD Fix")
    public static boolean gcdFix = true;

    // ── V2-Height Offset ────────────────────────────────────────────────────
    @RegisterSubModule(name = "Target Height", min = 0.1, max = 1.0, increment = 0.05)
    public static double targetHeight = 0.75;

    // ── V2-Predict (Velocity Extrapolation) ──────────────────────────────────
    @RegisterSubModule(name = "Predict")
    public static boolean predict = true;

    @RegisterSubModule(name = "Predict Amount", min = 0.0, max = 3.0, increment = 0.1, parent = "Predict")
    public static double predictAmount = 1.2;

    // ── V2-Randomize (Organic Micro-Sway & Noise) ───────────────────────────
    @RegisterSubModule(name = "Micro Sway")
    public static boolean microSway = true;

    @RegisterSubModule(name = "Sway Speed", min = 0.5, max = 4.0, increment = 0.1, parent = "Micro Sway")
    public static double swaySpeed = 1.5;

    @RegisterSubModule(name = "Sway Amplitude", min = 0.1, max = 2.0, increment = 0.1, parent = "Micro Sway")
    public static double swayAmplitude = 0.6;

    // ── V2-SmoothBack ───────────────────────────────────────────────────────
    @RegisterSubModule(name = "Smooth Back")
    public static boolean smoothBack = true;

    @RegisterSubModule(name = "Smooth Back Speed", min = 2.0, max = 30.0, increment = 1.0, parent = "Smooth Back")
    public static double smoothBackSpeed = 12.0;

    // ── Raytrace / FOV Check ────────────────────────────────────────────────
    @RegisterSubModule(name = "Raytrace Check")
    public static boolean raytraceCheck = true;

    @RegisterSubModule(name = "Max Attack Angle", min = 5.0, max = 180.0, increment = 5.0, parent = "Raytrace Check")
    public static double maxAttackAngle = 45.0;

    // ── State ───────────────────────────────────────────────────────────────
    @Getter public static EntityLivingBase target = null;
    private static final List<EntityLivingBase> targets = new ArrayList<>();
    private static long lastAttackTime = 0L;
    private static float serverYaw = 0.0f;
    private static float serverPitch = 0.0f;
    private static boolean shouldBlock = false;
    private static boolean isSmoothBacking = false;
    private static boolean hasActiveRotations = false;
    private static final SecureRandom random = new SecureRandom();

    // ── Rotation Event Processing ───────────────────────────────────────────
    @SubscribeEvent
    public static void onRotation(RotationEvent event) {
        if (C.p() == null || C.w() == null) {
            target = null;
            return;
        }

        updateTargets();
        EntityLivingBase bestTarget = findBestTarget();

        // Target state transition
        if (bestTarget != null) {
            target = bestTarget;
            isSmoothBacking = false;
        } else {
            if (target != null && smoothBack) {
                isSmoothBacking = true;
            }
            target = null;
        }

        // Initialize server yaw/pitch from client if not currently tracking
        if (!hasActiveRotations) {
            serverYaw = C.p().rotationYaw;
            serverPitch = C.p().rotationPitch;
            hasActiveRotations = true;
        }

        if (target != null) {
            // Target Aim Calculations with Predict & Height Offset
            float[] idealRot = calculateIdealRotations(target);
            float idealYaw = idealRot[0];
            float idealPitch = idealRot[1];

            // Apply organic micro-sway and subtle noise
            if (microSway) {
                float[] sway = computeOrganicSway();
                idealYaw += sway[0];
                idealPitch = MathHelper.clamp_float(idealPitch + sway[1], -90.0f, 90.0f);
            }

            // Human-like Non-Linear Smoothing with power curve ((Δθ / 45)^0.4)
            stepRotationTowards(idealYaw, idealPitch, rotationSpeed, acceleration);

            // Output to event and sync player visuals
            event.rotation = new RotationUtil.Rotation(serverPitch, serverYaw);
            if (rotationMode == RotationMode.Normal) {
                C.p().rotationYaw = serverYaw;
                C.p().rotationPitch = serverPitch;
            } else {
                C.p().rotationYawHead = serverYaw;
                C.p().renderYawOffset = serverYaw;
            }
        } else if (isSmoothBacking) {
            // Smoothly ease server rotation back to the player's client camera look
            float clientYaw = C.p().rotationYaw;
            float clientPitch = C.p().rotationPitch;

            float deltaYaw = wrapAngle(clientYaw - serverYaw);
            float deltaPitch = clientPitch - serverPitch;
            float dist = (float) Math.hypot(deltaYaw, deltaPitch);

            if (dist > 0.8f) {
                stepRotationTowards(clientYaw, clientPitch, smoothBackSpeed, acceleration);
                event.rotation = new RotationUtil.Rotation(serverPitch, serverYaw);
                C.p().rotationYawHead = serverYaw;
                C.p().renderYawOffset = serverYaw;
            } else {
                serverYaw = clientYaw;
                serverPitch = clientPitch;
                isSmoothBacking = false;
            }
        } else {
            serverYaw = C.p().rotationYaw;
            serverPitch = C.p().rotationPitch;
        }
    }

    /**
     * Calculates ideal yaw and pitch to target with velocity extrapolation (V2-Predict)
     * and configurable target height offset (V2-Height).
     */
    private static float[] calculateIdealRotations(EntityLivingBase entity) {
        double eyeX = C.p().posX;
        double eyeY = C.p().posY + C.p().getEyeHeight();
        double eyeZ = C.p().posZ;

        // Base aim position: feet + height * targetHeight
        double targetAimY = entity.posY + (entity.height * targetHeight);
        double targetAimX = entity.posX;
        double targetAimZ = entity.posZ;

        // Velocity extrapolation
        if (predict) {
            double deltaX = MathHelper.clamp_double(entity.posX - entity.lastTickPosX, -1.5, 1.5);
            double deltaY = MathHelper.clamp_double(entity.posY - entity.lastTickPosY, -1.0, 1.0);
            double deltaZ = MathHelper.clamp_double(entity.posZ - entity.lastTickPosZ, -1.5, 1.5);

            targetAimX += deltaX * predictAmount;
            targetAimY += deltaY * (predictAmount * 0.5); // dampened Y lead
            targetAimZ += deltaZ * predictAmount;
        }

        double diffX = targetAimX - eyeX;
        double diffY = targetAimY - eyeY;
        double diffZ = targetAimZ - eyeZ;
        double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distXZ));

        return new float[]{ wrapAngle(yaw), MathHelper.clamp_float(pitch, -90.0F, 90.0F) };
    }

    /**
     * Generates organic Lissajous harmonic micro-sway and Gaussian noise.
     */
    private static float[] computeOrganicSway() {
        long time = System.currentTimeMillis();
        double t = (time % 1000000L) / 1000.0 * swaySpeed;

        double harmonicYaw = Math.sin(t * 1.7) * 0.45 + Math.cos(t * 2.8 + 0.9) * 0.35 + Math.sin(t * 0.8 + 2.1) * 0.2;
        double harmonicPitch = Math.cos(t * 2.1 + 0.4) * 0.35 + Math.sin(t * 3.4 + 1.7) * 0.25 + Math.cos(t * 1.1) * 0.15;

        double noiseYaw = random.nextGaussian() * 0.08;
        double noisePitch = random.nextGaussian() * 0.06;

        float swayYaw = (float) ((harmonicYaw + noiseYaw) * swayAmplitude);
        float swayPitch = (float) ((harmonicPitch + noisePitch) * (swayAmplitude * 0.7));

        return new float[]{ swayYaw, swayPitch };
    }

    /**
     * Steps current server rotations toward target angles using non-linear power curve
     * acceleration/deceleration ((Δθ / 45)^0.4) and Minecraft sensitivity GCD alignment.
     */
    private static void stepRotationTowards(float targetYaw, float targetPitch, double baseSpeed, double powerExp) {
        float deltaYaw = wrapAngle(targetYaw - serverYaw);
        float deltaPitch = targetPitch - serverPitch;
        float deltaTheta = (float) Math.hypot(deltaYaw, deltaPitch);

        if (deltaTheta < 0.0001f) return;

        // Power curve acceleration/deceleration: speed scales with (Δθ / 45)^0.4
        double normalized = Math.max(0.01, deltaTheta / 45.0);
        double curveFactor = Math.pow(normalized, powerExp);
        double maxTurn = baseSpeed * curveFactor * (1.0 + (random.nextDouble() * 0.08 - 0.04));

        float stepRatio = deltaTheta <= maxTurn ? 1.0F : (float) (maxTurn / deltaTheta);
        float desiredStepYaw = deltaYaw * stepRatio;
        float desiredStepPitch = deltaPitch * stepRatio;

        if (gcdFix) {
            // Exact Minecraft mouse sensitivity quantization:
            // f = sens * 0.6 + 0.2
            // gcd = f^3 * 1.2
            float sens = C.mc.gameSettings.mouseSensitivity;
            float f = sens * 0.6F + 0.2F;
            float gcd = f * f * f * 1.2F;
            if (gcd <= 0.0F) gcd = 0.001F;

            int mouseDeltaX = Math.round(desiredStepYaw / gcd);
            int mouseDeltaY = Math.round(desiredStepPitch / gcd);

            // Nudge small threshold steps so we don't stall below 0.5 GCD
            if (mouseDeltaX == 0 && Math.abs(desiredStepYaw) >= gcd * 0.28F) {
                mouseDeltaX = desiredStepYaw > 0 ? 1 : -1;
            }
            if (mouseDeltaY == 0 && Math.abs(desiredStepPitch) >= gcd * 0.28F) {
                mouseDeltaY = desiredStepPitch > 0 ? 1 : -1;
            }

            serverYaw = wrapAngle(serverYaw + (mouseDeltaX * gcd));
            serverPitch = MathHelper.clamp_float(serverPitch + (mouseDeltaY * gcd), -90.0F, 90.0F);
        } else {
            serverYaw = wrapAngle(serverYaw + desiredStepYaw);
            serverPitch = MathHelper.clamp_float(serverPitch + desiredStepPitch, -90.0F, 90.0F);
        }
    }

    public static float wrapAngle(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F)  angle -= 360.0F;
        if (angle < -180.0F)  angle += 360.0F;
        return angle;
    }

    // ── Motion & Attack Handling ────────────────────────────────────────────
    @SubscribeEvent
    public static void onMotion(MotionEvent event) {
        if (target == null) return;
        if (canAttack()) attackTarget();
        if (autoBlock)   handleAutoBlock();
    }

    @SubscribeEvent
    public static void onPlayerMotion(MotionEvent event) {
        // Compatibility hook for Bus
    }

    @SubscribeEvent
    public static void onRotationEvent(RotationEvent event) {
        // Dispatched by Bus; main logic runs in onRotation
    }

    // ── Silent MoveFix Integration ──────────────────────────────────────────
    @SubscribeEvent
    public static void onMovementInput(MovementInputEvent event) {
        if ((target == null && !isSmoothBacking) || moveFix == MoveFix.Off) return;

        float yaw = serverYaw;
        if (moveFix == MoveFix.Strict) {
            C.p().rotationYaw = yaw;
            C.p().rotationYawHead = yaw;
        } else if (moveFix == MoveFix.Silent) {
            fixMovementSilent(event, yaw);
        } else if (moveFix == MoveFix.ChangeLook) {
            C.p().rotationYaw = yaw;
            C.p().rotationYawHead = yaw;
            fixMovementSilent(event, yaw);
        }
    }

    /**
     * Corrects movement input relative to server rotation, preserving vanilla discrete WASD values.
     */
    private static void fixMovementSilent(MovementInputEvent event, float targetYaw) {
        float forward = event.movementInput.moveForward;
        float strafe = event.movementInput.moveStrafe;
        if (forward == 0.0F && strafe == 0.0F) return;

        float diffRad = (float) Math.toRadians(targetYaw - C.p().rotationYaw);
        float cos = MathHelper.cos(diffRad);
        float sin = MathHelper.sin(diffRad);

        float newForward = forward * cos + strafe * sin;
        float newStrafe  = strafe * cos - forward * sin;

        boolean sneaking = event.movementInput.sneak;
        float multiplier = sneaking ? 0.3F : 1.0F;

        event.movementInput.moveForward = MathHelper.clamp_float(Math.round(newForward), -1.0F, 1.0F) * multiplier;
        event.movementInput.moveStrafe  = MathHelper.clamp_float(Math.round(newStrafe),  -1.0F, 1.0F) * multiplier;
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

    // ── Target Selection ────────────────────────────────────────────────────
    private static void updateTargets() {
        targets.clear();
        if (C.w() == null || C.p() == null) return;
        for (Object o : C.w().loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase entity = (EntityLivingBase) o;
            if (!isValidTarget(entity)) continue;
            if (C.p().getDistanceToEntity(entity) <= range) {
                targets.add(entity);
            }
        }
        targets.sort(Comparator.comparingDouble(e -> C.p().getDistanceToEntity(e)));
    }

    public static boolean isValidTarget(EntityLivingBase entity) {
        if (entity == null || entity == C.p()) return false;
        if (entity.isDead || entity.getHealth() <= 0) return false;
        if (entity instanceof EntityArmorStand) return false;
        if (AntiBot.isBot(entity)) return false;
        if (AntiBotUtil.isTeam(entity, false)) return false;
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

    // ── Attack & Raytrace Logic ─────────────────────────────────────────────
    private static boolean canAttack() {
        if (target == null || C.p() == null) return false;
        if (System.currentTimeMillis() - lastAttackTime < (long)(1000.0 / cps)) return false;
        if (C.p().getDistanceToEntity(target) > range) return false;
        if (C.mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) return false;
        if (!isValidTarget(target)) return false;

        // Angle / Raytrace validation to ensure we are facing the target
        if (raytraceCheck) {
            float[] idealRot = calculateIdealRotations(target);
            float angleDelta = (float) Math.hypot(wrapAngle(idealRot[0] - serverYaw), idealRot[1] - serverPitch);
            if (angleDelta > maxAttackAngle) {
                return false; // Rotations still in flight; wait to prevent attacking out of field of view
            }
        }

        return true;
    }

    private static void attackTarget() {
        if (target == null) return;
        if (autoBlock) block();
        C.mc.playerController.attackEntity(C.p(), target);
        C.p().swingItem();
        lastAttackTime = System.currentTimeMillis();
        if (autoBlock && !shouldBlock) unblock();
    }

    // ── Auto Block ──────────────────────────────────────────────────────────
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

    // ── Lifecycle ───────────────────────────────────────────────────────────
    @Override
    protected void onEnable() {
        target             = null;
        lastAttackTime     = 0L;
        shouldBlock        = false;
        isSmoothBacking    = false;
        hasActiveRotations = false;
    }

    @Override
    protected void onDisable() {
        target             = null;
        shouldBlock        = false;
        isSmoothBacking    = false;
        hasActiveRotations = false;
        if (C.p() != null) unblock();
    }

    // ── Compatibility API for Bus, Mixins, PlayerUtil, FakeLag, TickBase ────
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

    public static void tickAutoBlock(PlayerUpdateEvent event) {}
    public static void tickSwingQueued(PlayerUpdateEvent event) {}
    public static void tryAttackTarget(PlayerUpdateEvent event) {}
}

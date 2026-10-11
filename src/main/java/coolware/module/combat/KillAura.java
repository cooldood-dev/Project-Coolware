package coolware.module.combat;

import coolware.module.Category;
import coolware.module.Module;
import coolware.module.movement.Scaffold;
import coolware.setting.*;
import coolware.util.*;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class KillAura extends Module {

    // ─── Settings ─────────────────────────────────────────────────────────────
    public static ModeSetting   cpsMode;
    public static ModeSetting   mode;
    public static ModeSetting   sort;
    public static ModeSetting   autoblockMode;  // "None" | "Fake"
    public static NumberSetting swingRange;
    public static NumberSetting attackRange;
    public static NumberSetting fov;
    public static NumberSetting minCPS;
    public static NumberSetting maxCPS;
    public static NumberSetting switchDelay;

    // ─── Backward-compat aliases (used by TargetUtil / MovementFixUtil) ───────
    /** @deprecated use botCheck */   public static BooleanSetting antiBot;
    /** @deprecated use botCheck */   public static ModeSetting    botPingMode;
    /** @deprecated use teams */      public static BooleanSetting teamsCheck;
    /** @deprecated use teams */      public static BooleanSetting clientTeams;
    /** @deprecated use teams */      public static BooleanSetting colourTeams;
    /** @deprecated use moveFix */    public static BooleanSetting movementFix;
    /** @deprecated use moveFix */    public static ModeSetting    moveFixMode;

    // Rotations
    public static ModeSetting   rotations;  // NONE/Legit/Silent/LockView/LiquidBounce/Hypixel
    // LiquidBounce sub-settings
    public static NumberSetting deadZoneSize;
    public static NumberSetting maxTurnSpeed;
    public static NumberSetting minTurnSpeed;
    public static NumberSetting acceleration;
    public static NumberSetting deceleration;
    public static BooleanSetting useOvershoot;
    public static NumberSetting overshootStrength;
    public static NumberSetting overshootRecovery;
    public static NumberSetting noiseStrength;
    public static BooleanSetting randomize;
    public static NumberSetting randomizeRange;
    public static NumberSetting yRandomizeStrength;
    public static BooleanSetting visualizeAim;
    public static BooleanSetting smoothBack;
    public static NumberSetting liquidBounceHorizontalSpeed;
    public static NumberSetting liquidBounceVerticalSpeed;
    public static NumberSetting liquidBounceSmoothFactor;
    public static BooleanSetting liquidBouncePredict;
    public static NumberSetting liquidBouncePredictSize;
    public static BooleanSetting liquidBounceRandomize;
    public static NumberSetting liquidBounceRandomizeRange;
    public static NumberSetting liquidBounceHorizontalSearch;
    public static NumberSetting liquidBounceBodyPointMin;
    public static NumberSetting liquidBounceBodyPointMax;
    // Raven/Hypixel sub-settings
    public static NumberSetting ravenSmoothing;
    public static NumberSetting ravenPredictTicks;
    public static NumberSetting ravenYawRandom;
    public static NumberSetting angleStep;
    public static NumberSetting smoothing;

    // Targeting filters
    public static BooleanSetting throughWalls;
    public static BooleanSetting requirePress;
    public static BooleanSetting allowMining;
    public static BooleanSetting weaponsOnly;
    public static BooleanSetting allowTools;
    public static BooleanSetting inventoryCheck;
    public static BooleanSetting botCheck;
    public static BooleanSetting players;
    public static BooleanSetting bosses;
    public static BooleanSetting mobs;
    public static BooleanSetting animals;
    public static BooleanSetting golems;
    public static BooleanSetting silverfish;
    public static BooleanSetting teams;

    // MoveFix
    public static ModeSetting   moveFix;        // "NONE" | "Silent" | "Strict"

    // ShowTarget
    public static ModeSetting   showTarget;

    // ─── Runtime State ────────────────────────────────────────────────────────
    private static final long ROTATION_UPDATE_INTERVAL_MS = 50L;
    private static final int[] CLICK_PATTERN = {
        16,22,14,46,18,8,8,63,25,25,12,39,26,18,6,62,26,18,21,40,26,8,16,46,
        26,20,15,50,25,10,11,43,25,11,37,39,25,12,18,54,25,25,15,41,27,9,1,66,
        26,17,21,48,27,8,6,62,28,19,13,47,26,7,14,53,27,16,29,38,27,8,6,60,
        27,22,19,45,26,10,10,62,25,20,28,22,26,19,11,57,26,16,32,36,26,9,9,66,
        27,19,27,38,26,9,10,61,26,25,15,34,26,20,10,52,26,22,28,29,27,8,3,63,
        26,21,27,38,26,10,11,38,27,15,31,39,25,13,10,45,27,14,27,40,26,10,6,51,
        26,18,31,27,27,11,14,47,26,23,21,35,26,12,13,41,26,15,31,36,27,16,9,44
    };

    // Current attack target
    public static AttackData target = null;

    // Rotation state
    private float serverYaw   = 0f;
    private float serverPitch = 0f;
    private boolean isSmoothBacking = false;
    private boolean wantsToDisable  = false;
    private Vec3    currentAimVec   = null;
    private long    lastRotationUpdateTime = 0L;

    // Attack/block state
    private long   attackDelayMS  = 0L;
    private int    patternIndex   = 0;
    private int    switchTick     = 0;
    private boolean hitRegistered = false;
    private boolean isBlocking    = false;
    private boolean fakeBlockState = false;
    private boolean blockingState  = false;
    public static int    blockTick       = 0;

    // Switch timer
    private long   lastSwitchTime  = 0L;

    private final Random random = new Random();

    // ─── Constructor ──────────────────────────────────────────────────────────
    public KillAura() {
        super("KillAura", "Full-featured kill aura ported from OpenMyau-Plus", Category.COMBAT, 37); // K

        // CPS Mode
        cpsMode   = new ModeSetting("CPS Mode",   this, "Normal",  "Normal", "Record");
        // Attack Mode
        mode      = new ModeSetting("Mode",        this, "Switch",  "Single", "Switch");
        // Sort
        sort      = new ModeSetting("Sort",        this, "Health",  "Distance","Health","HurtTime","FOV");
        // AutoBlock — ONLY None and Fake
        autoblockMode = new ModeSetting("AutoBlock", this, "Fake",   "None", "Fake");

        // Range
        swingRange  = new NumberSetting("Swing Range",  this, 3.0, 3.0, 6.0, 0.05);
        attackRange = new NumberSetting("Attack Range", this, 3.0, 3.0, 6.0, 0.05);
        fov         = new NumberSetting("FOV",          this, 360.0, 30.0, 360.0, 1.0);
        minCPS      = new NumberSetting("Min CPS",      this, 14.0, 1.0, 20.0, 1.0);
        maxCPS      = new NumberSetting("Max CPS",      this, 14.0, 1.0, 20.0, 1.0);
        switchDelay = new NumberSetting("Switch Delay", this, 150.0, 0.0, 1000.0, 10.0);

        // Rotations
        rotations = new ModeSetting("Rotations", this, "Silent",
                "NONE", "Legit", "Silent", "LockView", "LiquidBounce", "Hypixel");

        // LiquidBounce rotation settings (Hidden from ClickGUI)
        deadZoneSize              = new NumberSetting("LB DeadZone",    this, 0.5,  0.0,  2.0,  0.05);
        deadZoneSize.setHidden(true);
        maxTurnSpeed              = new NumberSetting("LB MaxSpeed",    this, 25.0, 5.0,  180.0, 1.0);
        maxTurnSpeed.setHidden(true);
        minTurnSpeed              = new NumberSetting("LB MinSpeed",    this, 5.0,  1.0,  90.0,  1.0);
        minTurnSpeed.setHidden(true);
        acceleration              = new NumberSetting("LB Accel",       this, 2.5,  0.1,  10.0,  0.1);
        acceleration.setHidden(true);
        deceleration              = new NumberSetting("LB Decel",       this, 1.5,  0.1,  10.0,  0.1);
        deceleration.setHidden(true);
        useOvershoot              = new BooleanSetting("LB Overshoot",  this, true);
        useOvershoot.setHidden(true);
        overshootStrength         = new NumberSetting("LB OverStr",     this, 5.0,  0.0,  20.0,  0.5);
        overshootStrength.setHidden(true);
        overshootRecovery         = new NumberSetting("LB OverRecov",   this, 0.2,  0.01, 1.0,   0.01);
        overshootRecovery.setHidden(true);
        noiseStrength             = new NumberSetting("LB Noise",       this, 0.2,  0.0,  2.0,   0.05);
        noiseStrength.setHidden(true);
        randomize                 = new BooleanSetting("LB Randomize",  this, true);
        randomize.setHidden(true);
        randomizeRange            = new NumberSetting("LB RandRange",   this, 0.4,  0.0,  1.0,   0.05);
        randomizeRange.setHidden(true);
        yRandomizeStrength        = new NumberSetting("LB YRandom",     this, 0.3,  0.0,  1.0,   0.05);
        yRandomizeStrength.setHidden(true);
        visualizeAim              = new BooleanSetting("Visualize Aim", this, true);
        visualizeAim.setHidden(true);
        smoothBack                = new BooleanSetting("Smooth Back",   this, true);
        smoothBack.setHidden(true);
        liquidBounceHorizontalSpeed = new NumberSetting("LB HSpeed",   this, 180.0, 1.0, 180.0, 1.0);
        liquidBounceHorizontalSpeed.setHidden(true);
        liquidBounceVerticalSpeed   = new NumberSetting("LB VSpeed",   this, 180.0, 1.0, 180.0, 1.0);
        liquidBounceVerticalSpeed.setHidden(true);
        liquidBounceSmoothFactor    = new NumberSetting("LB Smooth",   this, 0.5,   0.1, 1.0,   0.05);
        liquidBounceSmoothFactor.setHidden(true);
        liquidBouncePredict         = new BooleanSetting("LB Predict", this, true);
        liquidBouncePredict.setHidden(true);
        liquidBouncePredictSize     = new NumberSetting("LB PredSize", this, 1.0,   0.0, 3.0,   0.1);
        liquidBouncePredictSize.setHidden(true);
        liquidBounceRandomize       = new BooleanSetting("LB Rand",    this, true);
        liquidBounceRandomize.setHidden(true);
        liquidBounceRandomizeRange  = new NumberSetting("LB RandRng",  this, 0.5,   0.0, 1.0,   0.05);
        liquidBounceRandomizeRange.setHidden(true);
        liquidBounceHorizontalSearch= new NumberSetting("LB HSearch",  this, 0.5,   0.0, 1.0,   0.05);
        liquidBounceHorizontalSearch.setHidden(true);
        liquidBounceBodyPointMin    = new NumberSetting("LB BodyMin",  this, 0.1,   0.0, 1.0,   0.05);
        liquidBounceBodyPointMin.setHidden(true);
        liquidBounceBodyPointMax    = new NumberSetting("LB BodyMax",  this, 0.9,   0.0, 1.0,   0.05);
        liquidBounceBodyPointMax.setHidden(true);

        // Raven/Hypixel rotation settings
        ravenSmoothing   = new NumberSetting("H Smoothing", this, 0.0, 0.0, 10.0, 1.0);
        ravenSmoothing.setHidden(true);
        ravenPredictTicks= new NumberSetting("H Predict",   this, 0.0, 0.0, 5.0,  1.0);
        ravenPredictTicks.setHidden(true);
        ravenYawRandom   = new NumberSetting("H YawRandom", this, 0.0, 0.0, 5.0,  1.0);
        angleStep        = new NumberSetting("Angle Step",  this, 180.0, 30.0, 180.0, 1.0);
        smoothing        = new NumberSetting("Smoothing",   this, 0.0,  0.0, 100.0, 1.0);

        // Targeting (Hidden in ClickGUI)
        throughWalls  = new BooleanSetting("Through Walls",    this, true);
        throughWalls.setHidden(true);
        requirePress  = new BooleanSetting("Require Press",    this, false);
        requirePress.setHidden(true);
        allowMining   = new BooleanSetting("Allow Mining",     this, true);
        allowMining.setHidden(true);
        weaponsOnly   = new BooleanSetting("Weapons Only",     this, false);
        weaponsOnly.setHidden(true);
        allowTools    = new BooleanSetting("Allow Tools",      this, true);
        allowTools.setHidden(true);
        inventoryCheck= new BooleanSetting("Inventory Check",  this, true);
        inventoryCheck.setHidden(true);
        botCheck      = new BooleanSetting("Bot Check",        this, false);
        botCheck.setHidden(true);
        players       = new BooleanSetting("Players",          this, true);
        players.setHidden(true);
        bosses        = new BooleanSetting("Bosses",           this, false);
        bosses.setHidden(true);
        mobs          = new BooleanSetting("Mobs",             this, true);
        mobs.setHidden(true);
        animals       = new BooleanSetting("Animals",          this, false);
        animals.setHidden(true);
        golems        = new BooleanSetting("Golems",           this, false);
        golems.setHidden(true);
        silverfish    = new BooleanSetting("Silverfish",       this, false);
        silverfish.setHidden(true);
        teams         = new BooleanSetting("Teams",            this, false);
        teams.setHidden(true);

        // MoveFix
        moveFix   = new ModeSetting("MoveFix", this, "Silent", "NONE", "Silent", "Strict");

        // ShowTarget
        showTarget = new ModeSetting("Show Target", this, "Default", "NONE", "Default");

        // Register all settings
        addSetting(cpsMode);
        addSetting(mode);
        addSetting(sort);
        addSetting(autoblockMode);
        addSetting(swingRange);
        addSetting(attackRange);
        addSetting(fov);
        addSetting(minCPS);
        addSetting(maxCPS);
        addSetting(switchDelay);
        addSetting(rotations);
        addSetting(deadZoneSize);
        addSetting(maxTurnSpeed);
        addSetting(minTurnSpeed);
        addSetting(acceleration);
        addSetting(deceleration);
        addSetting(useOvershoot);
        addSetting(overshootStrength);
        addSetting(overshootRecovery);
        addSetting(noiseStrength);
        addSetting(randomize);
        addSetting(randomizeRange);
        addSetting(yRandomizeStrength);
        addSetting(visualizeAim);
        addSetting(smoothBack);
        addSetting(liquidBounceHorizontalSpeed);
        addSetting(liquidBounceVerticalSpeed);
        addSetting(liquidBounceSmoothFactor);
        addSetting(liquidBouncePredict);
        addSetting(liquidBouncePredictSize);
        addSetting(liquidBounceRandomize);
        addSetting(liquidBounceRandomizeRange);
        addSetting(liquidBounceHorizontalSearch);
        addSetting(liquidBounceBodyPointMin);
        addSetting(liquidBounceBodyPointMax);
        addSetting(ravenSmoothing);
        addSetting(ravenPredictTicks);
        addSetting(ravenYawRandom);
        addSetting(angleStep);
        addSetting(smoothing);
        addSetting(throughWalls);
        addSetting(requirePress);
        addSetting(allowMining);
        addSetting(weaponsOnly);
        addSetting(allowTools);
        addSetting(inventoryCheck);
        addSetting(botCheck);
        addSetting(players);
        addSetting(bosses);
        addSetting(mobs);
        addSetting(animals);
        addSetting(golems);
        addSetting(silverfish);
        addSetting(teams);
        addSetting(moveFix);
        addSetting(showTarget);

        // Wire backward-compat aliases so TargetUtil / MovementFixUtil still work
        antiBot      = botCheck;          // null-safe: if botCheck is null, antiBot is null too
        botPingMode  = null;              // No ping-mode in new KA; TargetUtil handles null
        teamsCheck   = teams;
        clientTeams  = teams;
        colourTeams  = teams;
        // movementFix is a BooleanSetting wrapper that is true when moveFix != "NONE"
        // MovementFixUtil reads KillAura.movementFix.isEnabled()
        movementFix  = new BooleanSetting("_MFAlias", this, !moveFix.is("NONE")) {
            @Override public boolean isEnabled() { return moveFix != null && !moveFix.is("NONE"); }
        };
        moveFixMode  = moveFix;           // MovementFixUtil reads KillAura.moveFixMode.getMode()
    }


    // ─── Public API used by ItemRenderer / other modules ──────────────────────

    /** Returns true when fake-blocking animation should play (used by ItemRenderer). */
    public static boolean isBlocking() {
        return isFakeBlocking();
    }

    public static boolean isFakeBlocking() {
        for (coolware.module.Module m : coolware.Coolware.getInstance().getModuleManager().getModules()) {
            if (m instanceof KillAura) {
                KillAura ka = (KillAura) m;
                if (!ka.isToggled()) return false;
                return ka.fakeBlockState && ItemUtil.isHoldingSword();
            }
        }
        return false;
    }

    public static boolean isActivelyAiming() {
        for (coolware.module.Module m : coolware.Coolware.getInstance().getModuleManager().getModules()) {
            if (m instanceof KillAura) {
                KillAura ka = (KillAura) m;
                if (!ka.isToggled()) return false;
                if (ka.wantsToDisable || ka.isSmoothBacking) return true;
                return ka.target != null && ka.target.box != null && ka.isBoxInSwingRange(ka.target.box)
                        && rotations != null && !"NONE".equals(rotations.getMode());
            }
        }
        return false;
    }

    /** Called by EntityPlayerSP — kept for API compatibility, no-op for None/Fake modes */
    public static void onEntityStatusPacket(S19PacketEntityStatus packet) {
        // No watchdog mode — nothing needed here for None/Fake only
    }

    // ─── Module lifecycle ─────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        target          = null;
        switchTick      = 0;
        hitRegistered   = false;
        attackDelayMS   = 0L;
        blockTick       = 0;
        serverYaw       = mc.thePlayer != null ? mc.thePlayer.rotationYaw   : 0f;
        serverPitch     = mc.thePlayer != null ? mc.thePlayer.rotationPitch : 0f;
        currentAimVec   = null;
        isSmoothBacking = false;
        wantsToDisable  = false;
        lastRotationUpdateTime = 0L;
        patternIndex    = 0;
        lastSwitchTime  = 0L;
        if (!Scaffold.wantsMoveFix()) {
            RotationUtil.resetSilentRotation();
        }
    }

    @Override
    public void onDisable() {
        // If LiquidBounce + SmoothBack is on and we have a big rotation delta, defer disable
        if (isLiquidBounce() && smoothBack.isEnabled()) {
            float dyaw   = MathHelper.wrapAngleTo180_float(serverYaw   - mc.thePlayer.rotationYaw);
            float dpitch = MathHelper.wrapAngleTo180_float(serverPitch - mc.thePlayer.rotationPitch);
            if (Math.abs(dyaw) > 1.0F || Math.abs(dpitch) > 1.0F) {
                wantsToDisable = true;
                setToggled(true);
                return;
            }
        }
        if (!Scaffold.wantsMoveFix()) {
            RotationUtil.resetSilentRotation();
        }
        stopFakeBlock();
        stopBlockPacket();
        target          = null;
        currentAimVec   = null;
        wantsToDisable  = false;
        isSmoothBacking = false;
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // ── SmoothBack-disable sequence ──────────────────────────────────────
        if (wantsToDisable) {
            if (isLiquidBounce() && smoothBack.isEnabled()) {
                float[] sr = getSmoothBackRotation(serverYaw, serverPitch,
                        mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                serverYaw   = sr[0];
                serverPitch = sr[1];
                float dyaw   = MathHelper.wrapAngleTo180_float(serverYaw   - mc.thePlayer.rotationYaw);
                float dpitch = MathHelper.wrapAngleTo180_float(serverPitch - mc.thePlayer.rotationPitch);
                if (Math.abs(dyaw) > 1.0F || Math.abs(dpitch) > 1.0F) {
                    applySilentRotation(serverYaw, serverPitch);
                    return;
                }
            }
            wantsToDisable = false;
            setToggled(false);
            if (!Scaffold.wantsMoveFix()) {
                RotationUtil.resetSilentRotation();
            }
            stopFakeBlock();
            stopBlockPacket();
            return;
        }

        // ── Tick attack delay ─────────────────────────────────────────────────
        if (attackDelayMS > 0L) attackDelayMS -= 50L;

        // ── Init serverRotation if no target ─────────────────────────────────
        if (target == null && !isSmoothBacking) {
            serverYaw   = mc.thePlayer.rotationYaw;
            serverPitch = mc.thePlayer.rotationPitch;
            currentAimVec = null;
            if (!Scaffold.wantsMoveFix()) {
                RotationUtil.resetSilentRotation();
            }
        }

        // ── Build target list ─────────────────────────────────────────────────
        boolean needsNewTarget = false;
        if (target == null
                || !isValidTarget(target.entity)
                || !isInSwingRange(target.entity)
                || (System.currentTimeMillis() - lastSwitchTime) >= (long) switchDelay.getValue()) {
            needsNewTarget = true;
            lastSwitchTime = System.currentTimeMillis();
        }

        List<EntityLivingBase> validTargets = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase elb = (EntityLivingBase) entity;
                if (isValidTarget(elb) && isInRange(elb)) {
                    validTargets.add(elb);
                }
            }
        }

        if (validTargets.isEmpty()) {
            target        = null;
            currentAimVec = null;
        } else {
            // Prefer entities in swing range, then attack range, then player targets
            if (validTargets.stream().anyMatch(this::isInSwingRange))
                validTargets.removeIf(e -> !isInSwingRange(e));
            if (validTargets.stream().anyMatch(this::isInAttackRange))
                validTargets.removeIf(e -> !isInAttackRange(e));
            if (validTargets.stream().anyMatch(this::isPlayerTarget))
                validTargets.removeIf(e -> !isPlayerTarget(e));

            // Sort
            validTargets.sort((a, b) -> {
                int s = 0;
                switch (sort.getMode()) {
                    case "Health":   s = Float.compare(a.getHealth(), b.getHealth()); break;
                    case "HurtTime": s = Integer.compare(a.hurtResistantTime, b.hurtResistantTime); break;
                    case "FOV":      s = Float.compare(angleToEntity(a), angleToEntity(b)); break;
                    default: break;
                }
                return s != 0 ? s : Double.compare(distanceToEntity(a), distanceToEntity(b));
            });

            if (needsNewTarget) {
                if ("Switch".equals(mode.getMode()) && hitRegistered) {
                    hitRegistered = false;
                    switchTick++;
                }
                if ("Single".equals(mode.getMode()) || switchTick >= validTargets.size()) {
                    switchTick = 0;
                }
                EntityLivingBase newTarget = validTargets.get(switchTick);
                if (target == null || target.entity != newTarget) {
                    target = new AttackData(newTarget);
                }
            }
        }

        // Update target hitbox and coordinates every tick so tracking never goes stale
        if (target != null) {
            target.update();
        }

        // ── Combat logic ──────────────────────────────────────────────────────
        boolean canAtk = target != null && canAttack();
        boolean canAB  = target != null && canAutoBlock();

        if (!canAB) {
            isBlocking    = false;
            fakeBlockState= false;
            blockTick     = 0;
            stopBlockPacket();
        }

        boolean doSwap   = false;
        boolean attacked = false;

        // ── AutoBlock Fake mode ───────────────────────────────────────────
        if (canAB && autoblockMode.is("Fake")) {
            if (hasValidTarget()) {
                isBlocking     = true;
                fakeBlockState = true;
            } else {
                isBlocking     = false;
                fakeBlockState = false;
            }
        } else if (!canAB) {
            isBlocking     = false;
            fakeBlockState = false;
        }

        // ── Rotation + attack dispatch ────────────────────────────────────
        boolean hasAimTarget = target != null && target.box != null && isBoxInSwingRange(target.box);

        // If BedBreaker is rotating/breaking bed and player is NOT attacked, yield rotation to BedBreaker
        boolean isAttackedForRot = PlayerUtil.isBeingAttacked();
        if (coolware.module.player.BedBreaker.getCurrentTarget() != null
                && coolware.module.player.BedBreaker.shouldRotate()
                && !isAttackedForRot) {
            hasAimTarget = false;
        }

        if (hasAimTarget) {
            isSmoothBacking = false;
            String rot = rotations.getMode();

            if ("LiquidBounce".equals(rot)) {
                if (Float.isNaN(serverYaw) || Float.isNaN(serverPitch)) {
                    serverYaw   = mc.thePlayer.rotationYaw;
                    serverPitch = mc.thePlayer.rotationPitch;
                }
                float[] next = updateLiquidBounceRotation(serverYaw, serverPitch);
                float[] gcd  = applyGCD(new float[]{next[0], next[1]}, new float[]{serverYaw, serverPitch});
                serverYaw   = gcd[0];
                serverPitch = gcd[1];
                updateRenderAimPosition(serverYaw, serverPitch);
                applySilentRotation(serverYaw, serverPitch);
                mc.thePlayer.rotationYawHead  = serverYaw;
                mc.thePlayer.renderYawOffset  = serverYaw;
                if (canAtk) attacked = performAttack(serverYaw, serverPitch);

            } else if ("Hypixel".equals(rot)) {
                if (Float.isNaN(serverYaw) || Float.isNaN(serverPitch)) {
                    serverYaw   = mc.thePlayer.rotationYaw;
                    serverPitch = mc.thePlayer.rotationPitch;
                }
                float[] raw      = getRavenRotations(target.entity);
                float[] fixedR   = ravenFixRotation(raw[0], raw[1], serverYaw, serverPitch);
                float[] smoothed = getRavenRotationsSmoothed(fixedR, serverYaw, serverPitch);
                float fy = smoothed[0];
                float fp = MathHelper.clamp_float(smoothed[1], -90f, 90f);
                serverYaw   = fy;
                serverPitch = fp;
                applySilentRotation(fy, fp);
                mc.thePlayer.rotationYawHead  = fy;
                mc.thePlayer.renderYawOffset  = fy;
                if (canAtk) attacked = performAttack(fy, fp);

            } else if (!"NONE".equals(rot)) {
                // Legit / Silent / LockView (ported 1:1 from OpenMyau-Plus KillAura)
                float randomYaw = (random.nextFloat() * 5.0F) - 2.5F;
                float randomPitch = (random.nextFloat() * 3.0F) - 1.5F;
                float[] rots = getRotationsToBox(
                        target.box,
                        serverYaw,
                        serverPitch,
                        (float) angleStep.getValue() + ((random.nextFloat() * 10.0F) - 5.0F),
                        (float) smoothing.getValue() / 100.0F
                );
                float finalYaw = rots[0] + randomYaw;
                float finalPitch = rots[1] + randomPitch;

                // GCD FIX
                float[] fixed = applyGCD(new float[]{finalYaw, finalPitch}, new float[]{serverYaw, serverPitch});
                finalYaw = fixed[0];
                finalPitch = MathHelper.clamp_float(fixed[1], -90.0F, 90.0F);

                serverYaw   = finalYaw;
                serverPitch = finalPitch;
                applySilentRotation(finalYaw, finalPitch);
                if ("LockView".equals(rot)) {
                    mc.thePlayer.rotationYaw   = finalYaw;
                    mc.thePlayer.rotationPitch = finalPitch;
                } else {
                    mc.thePlayer.rotationYawHead  = finalYaw;
                    mc.thePlayer.renderYawOffset  = finalYaw;
                }
                if (canAtk) attacked = performAttack(finalYaw, finalPitch);

            } else {
                // NONE — attack at current look
                if (!Scaffold.wantsMoveFix()) {
                    RotationUtil.resetSilentRotation();
                }
                if (canAtk) attacked = performAttack(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            }
        } else {
            // No target in swing range (or can't attack)
            if (isLiquidBounce() && smoothBack.isEnabled() && RotationUtil.silentActive) {
                float dyaw   = MathHelper.wrapAngleTo180_float(serverYaw   - mc.thePlayer.rotationYaw);
                float dpitch = MathHelper.wrapAngleTo180_float(serverPitch - mc.thePlayer.rotationPitch);
                if (Math.abs(dyaw) > 1.0F || Math.abs(dpitch) > 1.0F) {
                    isSmoothBacking = true;
                    float[] sr = getSmoothBackRotation(serverYaw, serverPitch,
                            mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                    float[] gcd = applyGCD(new float[]{sr[0], sr[1]}, new float[]{serverYaw, serverPitch});
                    serverYaw   = gcd[0];
                    serverPitch = gcd[1];
                    applySilentRotation(serverYaw, serverPitch);
                } else {
                    isSmoothBacking = false;
                    serverYaw   = mc.thePlayer.rotationYaw;
                    serverPitch = mc.thePlayer.rotationPitch;
                    if (!Scaffold.wantsMoveFix()) {
                        RotationUtil.resetSilentRotation();
                    }
                }
            } else {
                isSmoothBacking = false;
                serverYaw   = mc.thePlayer.rotationYaw;
                serverPitch = mc.thePlayer.rotationPitch;
                if (!Scaffold.wantsMoveFix()) {
                    RotationUtil.resetSilentRotation();
                }
            }
        }
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.getRenderManager() == null || mc.getRenderViewEntity() == null) return;

        double viewX = mc.getRenderManager().viewerPosX;
        double viewY = mc.getRenderManager().viewerPosY;
        double viewZ = mc.getRenderManager().viewerPosZ;

        // Draw target box
        if (target != null && showTarget.is("Default")
                && mc.theWorld != null && mc.theWorld.loadedEntityList.contains(target.entity)) {
            Color color = target.entity.hurtTime > 0 ? new Color(255, 85, 85) : new Color(85, 255, 85);
            drawEntityBoundingBox(target.entity, color, viewX, viewY, viewZ);
        }

        // Draw LiquidBounce aim vector
        if (visualizeAim.isEnabled() && currentAimVec != null) {
            double x = currentAimVec.xCoord - viewX;
            double y = currentAimVec.yCoord - viewY;
            double z = currentAimVec.zCoord - viewZ;
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableDepth();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.color(0.0F, 1.0F, 1.0F, 0.8F);
            double s = 0.08;
            AxisAlignedBB bb = new AxisAlignedBB(x-s,y-s,z-s,x+s,y+s,z+s);
            RenderGlobal.drawSelectionBoundingBox(bb);
            GL11.glLineWidth(2.0F);
            RenderGlobal.drawOutlinedBoundingBox(bb, 0, 255, 0, 128);
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }

    /** Render a colored outline box around an entity. */
    private void drawEntityBoundingBox(EntityLivingBase entity, Color color, double viewX, double viewY, double viewZ) {
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        double x1 = bb.minX - viewX, y1 = bb.minY - viewY, z1 = bb.minZ - viewZ;
        double x2 = bb.maxX - viewX, y2 = bb.maxY - viewY, z2 = bb.maxZ - viewZ;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        float r = color.getRed()/255f, g = color.getGreen()/255f, b2 = color.getBlue()/255f;
        GlStateManager.color(r, g, b2, 0.5f);
        AxisAlignedBB relBB = new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
        RenderGlobal.drawOutlinedBoundingBox(relBB, color.getRed(), color.getGreen(), color.getBlue(), 120);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // ─── Attack helpers ────────────────────────────────────────────────────────

    private boolean performAttack(float yaw, float pitch) {
        if (attackDelayMS > 0L) return false;

        // For LiquidBounce: require hitbox intersection
        if (isLiquidBounce()) {
            if (rayTrace(target.box, yaw, pitch, (float) attackRange.getValue()) == null) return false;
        }

        attackDelayMS = getAttackDelay();
        mc.thePlayer.swingItem();

        if (cpsMode.is("Record")) {
            patternIndex = (patternIndex + 1) % CLICK_PATTERN.length;
        }

        if (!isLiquidBounce() && !"NONE".equals(rotations.getMode())) {
            boolean inRange = isInAttackRange(target.entity) || isBoxInAttackRange(target.box);
            if (!inRange && rayTrace(target.box, yaw, pitch, (float) attackRange.getValue()) == null) {
                return false;
            }
        }

        if (mc.thePlayer.isUsingItem() && !canAutoBlock()) {
            mc.playerController.onStoppedUsingItem(mc.thePlayer);
        }

        mc.playerController.attackEntity(mc.thePlayer, target.entity);
        hitRegistered = true;
        return true;
    }

    private long getAttackDelay() {
        if (cpsMode.is("Record")) {
            return CLICK_PATTERN[patternIndex % CLICK_PATTERN.length];
        }
        long hi = (long)(1000.0 / Math.max(1, minCPS.getValue()));
        long lo = (long)(1000.0 / Math.max(1, maxCPS.getValue()));
        if (hi <= lo) return lo;
        return lo + (long)(random.nextDouble() * (hi - lo));
    }

    private void sendUseItemPacket() {
        if (mc.thePlayer == null || mc.thePlayer.getHeldItem() == null) return;
        ItemStack item = mc.thePlayer.getHeldItem();
        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(item));
        mc.thePlayer.setItemInUse(item, item.getMaxItemUseDuration());
        blockingState = true;
    }

    private void stopBlockPacket() {
        if (blockingState) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            mc.thePlayer.stopUsingItem();
            blockingState = false;
        }
    }

    private void stopFakeBlock() {
        isBlocking    = false;
        fakeBlockState= false;
        blockTick     = 0;
    }

    private void interactAttack(float yaw, float pitch) {
        if (target == null) return;
        Vec3 hit = rayTrace(target.box, yaw, pitch, 8.0f);
        if (hit != null) {
            mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(
                    target.entity,
                    new Vec3(hit.xCoord - target.x, hit.yCoord - target.y, hit.zCoord - target.z)));
            mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(target.entity, C02PacketUseEntity.Action.INTERACT));
        }
        sendUseItemPacket();
    }

    // ─── Target filters ────────────────────────────────────────────────────────

    private boolean canAttack() {
        if (inventoryCheck.isEnabled() && mc.currentScreen instanceof GuiContainer) return false;
        if (weaponsOnly.isEnabled()) {
            boolean swordOk = ItemUtil.isHoldingSword();
            boolean toolOk  = allowTools.isEnabled() && ItemUtil.isHoldingTool();
            if (!swordOk && !toolOk) return false;
        }

        // When being attacked during bed break or general combat, auto-switch to sword
        if (PlayerUtil.isBeingAttacked()) {
            PlayerUtil.switchToSword();
        }

        // Check BedBreaker priority: if BedBreaker has a target and overrides KillAura,
        // BUT if the player is being attacked, KillAura is prioritized!
        boolean isAttacked = PlayerUtil.isBeingAttacked();
        if (coolware.module.player.BedBreaker.getCurrentTarget() != null
                && coolware.module.player.BedBreaker.isNoKillAura()
                && !isAttacked) {
            return false;
        }

        // Allow attack while hitting blocks if being attacked or if not mining normally
        if (mc.playerController.getIsHittingBlock() && !isAttacked) return false;
        if ((ItemUtil.isEating() || ItemUtil.isUsingBow()) && mc.thePlayer.isUsingItem()) return false;
        if (requirePress.isEnabled() && !org.lwjgl.input.Mouse.isButtonDown(0)) return false;
        if (!allowMining.isEnabled() && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && org.lwjgl.input.Mouse.isButtonDown(0) && !isAttacked) return false;
        return true;
    }

    private boolean canAutoBlock() {
        return ItemUtil.isHoldingSword();
    }

    public boolean shouldAutoBlock() {
        return isPlayerBlocking() && isBlocking && !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava();
    }

    public boolean isPlayerBlocking() {
        if (mc.thePlayer == null) return false;
        return (mc.thePlayer.isUsingItem() || blockingState) && ItemUtil.isHoldingSword();
    }

    private boolean hasValidTarget() {
        if (mc.theWorld == null) return false;
        for (Entity e : mc.theWorld.loadedEntityList) {
            if (e instanceof EntityLivingBase
                    && isValidTarget((EntityLivingBase) e)
                    && isInAttackRange((EntityLivingBase) e)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidTarget(EntityLivingBase e) {
        if (mc.theWorld == null || !mc.theWorld.loadedEntityList.contains(e)) return false;
        if (e == mc.thePlayer || e == mc.thePlayer.ridingEntity) return false;
        if (e == mc.getRenderViewEntity()) return false;
        if (mc.getRenderViewEntity() != null && e == mc.getRenderViewEntity().ridingEntity) return false;
        if (e.deathTime > 0) return false;
        if (fov.getValue() < 360.0 && angleToEntity(e) > (float) (fov.getValue() / 2.0)) return false;
        if (!throughWalls.isEnabled()) {
            Vec3 start = mc.thePlayer.getPositionEyes(1.0f);
            Vec3 end   = new Vec3(e.posX, e.posY + e.height * 0.5, e.posZ);
            if (mc.theWorld.rayTraceBlocks(start, end, false, true, false) != null) return false;
        }

        if (e instanceof EntityPlayer) {
            if (!players.isEnabled()) return false;
            if (isFriendPlayer((EntityPlayer) e)) return false;
            if (teams.isEnabled() && isSameTeam((EntityPlayer) e)) return false;
            if (botCheck.isEnabled() && isBot((EntityPlayer) e)) return false;
            return true;
        }
        if (e instanceof EntityDragon || e instanceof EntityWither) return bosses.isEnabled();
        if (e instanceof EntitySilverfish) return silverfish.isEnabled();
        if (e instanceof EntityMob || e instanceof EntitySlime) return mobs.isEnabled();
        if (e instanceof EntityAnimal || e instanceof EntityBat || e instanceof EntitySquid || e instanceof EntityVillager)
            return animals.isEnabled();
        if (e instanceof EntityIronGolem) return golems.isEnabled();
        return false;
    }

    private boolean isFriendPlayer(EntityPlayer p) {
        // No dedicated FriendManager in Coolware — skip friend check
        return false;
    }

    private boolean isSameTeam(EntityPlayer p) {
        net.minecraft.scoreboard.Team t1 = mc.thePlayer.getTeam();
        net.minecraft.scoreboard.Team t2 = p.getTeam();
        if (t1 == null || t2 == null) return false;
        return t1 == t2;
    }

    private boolean isBot(EntityPlayer p) {
        if (mc.isSingleplayer()) return false;
        if (!(p instanceof net.minecraft.client.entity.AbstractClientPlayer)) return false;
        net.minecraft.client.network.NetworkPlayerInfo info =
                ((net.minecraft.client.entity.AbstractClientPlayer) p).getPlayerInfo();
        if (info == null) return false;
        return info.getResponseTime() < 0;
    }

    private boolean isPlayerTarget(EntityLivingBase e) {
        return e instanceof EntityPlayer;
    }

    private boolean isInRange(EntityLivingBase e) {
        return isInSwingRange(e) || isInAttackRange(e);
    }

    private boolean isInSwingRange(EntityLivingBase e) {
        return distanceToEntity(e) <= swingRange.getValue() || (e.getEntityBoundingBox() != null && isBoxInSwingRange(e.getEntityBoundingBox()));
    }

    private boolean isBoxInSwingRange(AxisAlignedBB box) {
        return distanceToBox(box) <= swingRange.getValue();
    }

    private boolean isInAttackRange(EntityLivingBase e) {
        return distanceToEntity(e) <= attackRange.getValue() || (e.getEntityBoundingBox() != null && isBoxInAttackRange(e.getEntityBoundingBox()));
    }

    private boolean isBoxInAttackRange(AxisAlignedBB box) {
        return distanceToBox(box) <= attackRange.getValue();
    }

    private double distanceToEntity(EntityLivingBase e) {
        if (e.getEntityBoundingBox() != null) {
            return distanceToBox(e.getEntityBoundingBox());
        }
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 head = new Vec3(e.posX, e.posY + e.getEyeHeight(), e.posZ);
        return eyes.distanceTo(head);
    }

    private double distanceToBox(AxisAlignedBB box) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double cx = MathHelper.clamp_double(eyes.xCoord, box.minX, box.maxX);
        double cy = MathHelper.clamp_double(eyes.yCoord, box.minY, box.maxY);
        double cz = MathHelper.clamp_double(eyes.zCoord, box.minZ, box.maxZ);
        return eyes.distanceTo(new Vec3(cx, cy, cz));
    }

    private float angleToEntity(EntityLivingBase e) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double dx = e.posX - eyes.xCoord;
        double dz = e.posZ - eyes.zCoord;
        float ty  = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        return Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - ty));
    }

    // ─── Rotation helpers ─────────────────────────────────────────────────────

    private boolean isLiquidBounce() {
        return rotations != null && "LiquidBounce".equals(rotations.getMode());
    }

    private void applySilentRotation(float yaw, float pitch) {
        RotationUtil.setSilentRotation(yaw, MathHelper.clamp_float(pitch, -90f, 90f));
    }

    private float[] updateLiquidBounceRotation(float cy, float cp) {
        if (target == null || System.currentTimeMillis() - lastRotationUpdateTime < ROTATION_UPDATE_INTERVAL_MS) {
            return new float[]{cy, cp};
        }
        lastRotationUpdateTime = System.currentTimeMillis();

        EntityLivingBase te = target.entity;
        AxisAlignedBB bb = te.getEntityBoundingBox().expand(
                te.getCollisionBorderSize(), te.getCollisionBorderSize(), te.getCollisionBorderSize());
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);

        Vec3 tp = searchCenterPoint(bb, eyes);
        if (tp == null) {
            tp = new Vec3((bb.minX+bb.maxX)/2.0, (bb.minY+bb.maxY)/2.0, (bb.minZ+bb.maxZ)/2.0);
        }
        if (liquidBouncePredict.isEnabled()) {
            tp = applyPrediction(tp, te);
        }

        double dx = tp.xCoord - eyes.xCoord;
        double dy = tp.yCoord - eyes.yCoord;
        double dz = tp.zCoord - eyes.zCoord;
        double dist = MathHelper.sqrt_double(dx*dx + dz*dz);

        float ty = (float)(Math.atan2(dz,dx)*180.0/Math.PI) - 90f;
        float tp2= (float)(-(Math.atan2(dy,dist)*180.0/Math.PI));
        ty  = MathHelper.wrapAngleTo180_float(ty);
        tp2 = MathHelper.wrapAngleTo180_float(tp2);

        return limitAngleChange(cy, cp, ty, tp2);
    }

    private Vec3 searchCenterPoint(AxisAlignedBB bb, Vec3 eyes) {
        double scanRange = Math.max(attackRange.getValue(), swingRange.getValue());
        double attackRng = attackRange.getValue();
        double minBody   = liquidBounceBodyPointMin.getValue();
        double maxBody   = liquidBounceBodyPointMax.getValue();
        double hMax      = liquidBounceHorizontalSearch.getValue();

        Vec3   best      = null;
        double bestScore = Double.MAX_VALUE;

        for (double x = 0; x <= hMax; x += 0.25) {
            for (double y = minBody; y <= maxBody; y += 0.25) {
                for (double z = 0; z <= hMax; z += 0.25) {
                    Vec3 pt = new Vec3(
                            bb.minX + (bb.maxX - bb.minX) * x,
                            bb.minY + (bb.maxY - bb.minY) * y,
                            bb.minZ + (bb.maxZ - bb.minZ) * z);
                    double distance = eyes.distanceTo(pt);
                    if (distance > scanRange) continue;
                    boolean visible = throughWalls.isEnabled() ||
                            mc.theWorld.rayTraceBlocks(eyes, pt, false, true, false) == null;
                    if (!visible && distance > attackRng) continue;
                    double score = distance;
                    if (distance > attackRng) score += 10.0;
                    if (!visible) score += 5.0;
                    if (liquidBounceRandomize.isEnabled()) {
                        score += random.nextDouble() * liquidBounceRandomizeRange.getValue() * 5.0;
                    }
                    if (score < bestScore) { bestScore = score; best = pt; }
                }
            }
        }
        return best;
    }

    private Vec3 applyPrediction(Vec3 point, EntityLivingBase entity) {
        double size = liquidBouncePredictSize.getValue();
        double px = entity.posX + entity.motionX * size;
        double py = entity.posY + entity.motionY * size * 0.5;
        double pz = entity.posZ + entity.motionZ * size;
        return new Vec3(px + (point.xCoord - entity.posX),
                        py + (point.yCoord - entity.posY),
                        pz + (point.zCoord - entity.posZ));
    }

    private float[] limitAngleChange(float cy, float cp, float ty, float tp) {
        float maxH  = (float) liquidBounceHorizontalSpeed.getValue();
        float maxV  = (float) liquidBounceVerticalSpeed.getValue();
        float smooth= (float) liquidBounceSmoothFactor.getValue();
        float yd = MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(ty - cy), -maxH, maxH) * smooth;
        float pd = MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(tp - cp), -maxV, maxV) * smooth;
        return new float[]{cy + yd, MathHelper.clamp_float(cp + pd, -90f, 90f)};
    }

    private float[] getSmoothBackRotation(float cy, float cp, float ty, float tp) {
        float yd    = MathHelper.wrapAngleTo180_float(ty - cy);
        float pd    = MathHelper.wrapAngleTo180_float(tp - cp);
        float speed = Math.max(5f, Math.abs(yd) * 0.3f);
        float ys    = MathHelper.clamp_float(yd, -speed, speed);
        float ps    = MathHelper.clamp_float(pd, -speed, speed);
        return new float[]{cy + ys, cp + ps};
    }

    private void updateRenderAimPosition(float yaw, float pitch) {
        if (mc.thePlayer == null || target == null) { currentAimVec = null; return; }
        float yr = yaw   * (float)Math.PI / 180f;
        float pr = pitch * (float)Math.PI / 180f;
        double lx = -Math.sin(yr) * Math.cos(pr);
        double ly = -Math.sin(pr);
        double lz =  Math.cos(yr) * Math.cos(pr);
        Vec3 ep   = mc.thePlayer.getPositionEyes(1.0f);
        double rd = Math.min(4.0, Math.max(0.1, mc.thePlayer.getDistanceToEntity(target.entity) * 0.5));
        currentAimVec = new Vec3(ep.xCoord + lx*rd, ep.yCoord + ly*rd, ep.zCoord + lz*rd);
    }

    // ─── Raven BS / Hypixel rotations ─────────────────────────────────────────

    private float[] getRavenRotations(EntityLivingBase entity) {
        int ticks = (int) ravenPredictTicks.getValue();
        double px = entity.posX, pz = entity.posZ;
        if (ticks > 0) {
            double dX = entity.posX - entity.lastTickPosX;
            double dZ = entity.posZ - entity.lastTickPosZ;
            for (int i = 0; i < ticks; i++) { px += dX; pz += dZ; }
        }
        double dx = px - mc.thePlayer.posX;
        double dz = pz - mc.thePlayer.posZ;
        double dy = entity.posY + entity.getEyeHeight() * 0.9 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        float yaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(
                (float)(Math.atan2(dz,dx)*57.2957763671875) - 90f - mc.thePlayer.rotationYaw);
        float pitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(
                (float)(-(Math.atan2(dy, MathHelper.sqrt_double(dx*dx+dz*dz))*57.2957763671875))
                - mc.thePlayer.rotationPitch) + 3f, -90f, 90f);
        return new float[]{yaw, pitch};
    }

    private float[] ravenFixRotation(float ty, float tp, float y, float p) {
        float n5 = ty - y, abs = Math.abs(n5), n7 = tp - p;
        float n8 = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        double n9 = (double)n8 * n8 * n8 * 1.2;
        float n10 = (float)(Math.round((double)n5/n9)*n9);
        float n11 = (float)(Math.round((double)n7/n9)*n9);
        ty = y + n10; tp = p + n11;
        if (abs >= 1.0f) {
            int factor = (int) ravenYawRandom.getValue();
            if (factor != 0) {
                int n13 = factor*100 + (random.nextInt(61) - 30);
                ty += (random.nextInt(2*Math.abs(n13)+1) - Math.abs(n13)) / 100f;
            }
        } else if (abs <= 0.04f) {
            ty += (abs > 0f) ? 0.01f : -0.01f;
        }
        return new float[]{ty, MathHelper.clamp_float(tp, -90f, 90f)};
    }

    private float[] getRavenRotationsSmoothed(float[] rots, float sy, float sp) {
        float uw = sy + (((rots[0]-sy+180f)%360f+360f)%360f-180f);
        float dy = uw - sy, dp = rots[1] - sp;
        float ys = (float) ravenSmoothing.getValue(), ps = ys;
        float strafe = mc.thePlayer.moveStrafing;
        if ((strafe < 0 && dy < 0) || (strafe > 0 && dy > 0)) ys = Math.max(1f, ys/2f);
        float my = (float) mc.thePlayer.motionY;
        if ((my > 0 && dp > 0) || (my < 0 && dp < 0)) ps = Math.max(1f, ps/2f);
        sy += dy / Math.max(1f, ys);
        sp += dp / Math.max(1f, ps);
        return new float[]{sy, sp};
    }

    // ─── General rotation math helpers (ported 1:1 from OpenMyau-Plus RotationUtil) ───

    private float clampAngle(float angle, float maxAngle) {
        maxAngle = Math.max(0.0f, Math.min(180.0f, maxAngle));
        if (angle > maxAngle) {
            angle = maxAngle;
        } else if (angle < -maxAngle) {
            angle = -maxAngle;
        }
        return angle;
    }

    private float smoothAngle(float angle, float smoothFactor) {
        float randSpread = (random.nextFloat() * 0.2f) - 0.1f;
        return angle * (0.5f + 0.5f * (1.0f - Math.max(0.0f, Math.min(1.0f, smoothFactor + randSpread))));
    }

    private float quantizeAngle(float angle) {
        return (float) ((double) angle - (double) angle % 0.0096f);
    }

    private float[] getRotationsToBox(AxisAlignedBB boundingBox, float yaw, float pitch, float maxAngle, float smoothFactor) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        double minTargetY = boundingBox.minY + 0.05 * (boundingBox.maxY - boundingBox.minY);
        double maxTargetY = boundingBox.minY + 0.75 * (boundingBox.maxY - boundingBox.minY);
        double deltaX = (boundingBox.minX + boundingBox.maxX) / 2.0 - eyePos.xCoord;
        double deltaY = eyePos.yCoord >= maxTargetY ? maxTargetY - eyePos.yCoord : (eyePos.yCoord <= minTargetY ? minTargetY - eyePos.yCoord : 0.0);
        double deltaZ = (boundingBox.minZ + boundingBox.maxZ) / 2.0 - eyePos.zCoord;

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yawDelta = MathHelper.wrapAngleTo180_float((float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f - yaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float((float) (-Math.atan2(deltaY, horizontalDistance) * 180.0 / Math.PI) - pitch);

        yawDelta = Math.abs(yawDelta) <= 1.0f ? 0.0f : smoothAngle(clampAngle(yawDelta, maxAngle), smoothFactor);
        pitchDelta = Math.abs(pitchDelta) <= 1.0f ? 0.0f : smoothAngle(clampAngle(pitchDelta, maxAngle), smoothFactor);

        return new float[]{quantizeAngle(yaw + yawDelta), quantizeAngle(pitch + pitchDelta)};
    }

    private float[] applyGCD(float[] rotation, float[] prev) {
        if (mc.gameSettings == null) return rotation;
        float mouseSensitivity = (float) (mc.gameSettings.mouseSensitivity * (1.0 + Math.random() / 10000000.0) * 0.6F + 0.2F);
        double mult = mouseSensitivity * mouseSensitivity * mouseSensitivity * 1.2F;

        float deltaYaw = rotation[0] - prev[0];
        float deltaPitch = rotation[1] - prev[1];

        float patchedYaw = prev[0] + (float) (Math.round((double) deltaYaw / mult) * mult);
        float patchedPitch = prev[1] + (float) (Math.round((double) deltaPitch / mult) * mult);

        return new float[]{patchedYaw, MathHelper.clamp_float(patchedPitch, -90.0F, 90.0F)};
    }

    /** Ray-trace against an AABB from player eyes at given yaw/pitch, returning hit vec or null. */
    private Vec3 rayTrace(AxisAlignedBB box, float yaw, float pitch, float range) {
        float yr = (float) Math.toRadians(yaw);
        float pr = (float) Math.toRadians(pitch);
        double lx = -Math.sin(yr) * Math.cos(pr);
        double ly = -Math.sin(pr);
        double lz =  Math.cos(yr) * Math.cos(pr);
        Vec3 start = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 end   = start.addVector(lx*range, ly*range, lz*range);
        MovingObjectPosition mop = box.calculateIntercept(start, end);
        return mop != null ? mop.hitVec : null;
    }

    public static EntityLivingBase getTargetEntity() {
        return target != null ? target.entity : null;
    }

    // ─── AttackData inner class ────────────────────────────────────────────────

    public static class AttackData {
        public final EntityLivingBase entity;
        public AxisAlignedBB box;
        public double x, y, z;

        public AttackData(EntityLivingBase entity) {
            this.entity = entity;
            update();
        }

        public void update() {
            double border = entity.getCollisionBorderSize();
            this.box = entity.getEntityBoundingBox().expand(border, border, border);
            this.x   = entity.posX;
            this.y   = entity.posY;
            this.z   = entity.posZ;
        }
    }
}

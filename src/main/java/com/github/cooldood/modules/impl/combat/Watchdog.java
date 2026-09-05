package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.bridge.net.minecraft.client.MinecraftBridge;
import com.github.cooldood.bridge.net.minecraft.client.settings.KeyBindingBridge;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.ClientTickEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.events.impl.RespawnEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.modules.impl.player.Fucker;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.MovementUtil;
import com.github.cooldood.utils.minecraft.PacketUtil;
import com.github.cooldood.utils.minecraft.PlayerUtil;
import com.github.cooldood.utils.minecraft.TargetUtil;
import lombok.Getter;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.Comparator;
import java.util.List;

@RegisterModule(
        name = "Watchdog",
        description = "Legitimate-feeling combat AutoBlock based on Enemy_IFrame with burst attack cycles.",
        category = Category.COMBAT
)
public class Watchdog extends Module {

    // ─── Range Settings ───────────────────────────────────────────────────────
    @RegisterSubModule(name = "Range", min = 1.0, max = 6.0)
    public static double range = 3.0;

    @RegisterSubModule(name = "Through Walls")
    public static boolean throughWalls = false;

    // ─── Mode Settings ────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Mode")
    public static Mode mode = Mode.Enemy_IFrame;

    public enum Mode {
        Enemy_IFrame,
        Smart,
        Player_IFrame
    }

    // ─── Speed & Rate Settings ────────────────────────────────────────────────
    @RegisterSubModule(name = "APS", min = 1.0, max = 20.0, increment = 1.0, description = "AutoBlocks Per Second (1 to 20 APS)")
    public static double aps = 15.0;

    @RegisterSubModule(name = "Attacks Per Unblock", min = 1, max = 5, description = "Number of precision attack swings during unblocked phase")
    public static int attacksPerUnblock = 3;

    // ─── Timing Thresholds ────────────────────────────────────────────────────
    @RegisterSubModule(name = "Target Hurt Threshold", min = 0, max = 20, description = "Target hurtResistantTime above which to block")
    public static int targetHurtThreshold = 10;

    @RegisterSubModule(name = "Player Hurt Threshold", min = 0, max = 10, description = "Local player hurtTime above which to block")
    public static int playerHurtThreshold = 0;

    // ─── Action Type ──────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Packet Mode", description = "Send C08/C07 packets directly instead of emulating keybind")
    public static boolean packetMode = false;

    // ─── State Management ─────────────────────────────────────────────────────
    @Getter
    private static boolean blocking = false;
    @Getter
    private static boolean serverBlocking = false;
    private static EntityLivingBase currentTarget = null;

    private static int attacksDoneInCycle = 0;
    private static boolean inAttackBurst = false;
    private static long lastCycleTime = 0;

    public static boolean isModuleBlocking() {
        return ModuleManager.isEnabled(Watchdog.class) && blocking;
    }

    @SubscribeEvent(priority = 997)
    public static void onPlayerUpdate(PlayerUpdateEvent event) {
        if (!C.isInGame() || C.p().isDead) {
            stopBlocking();
            return;
        }

        if (Fucker.getCurrentTarget() != null && Fucker.noAutoblock) {
            stopBlocking();
            return;
        }

        if (!isHoldingSword()) {
            stopBlocking();
            return;
        }

        currentTarget = findTarget();
        if (currentTarget == null) {
            stopBlocking();
            return;
        }

        tickAutoBlockLogic(currentTarget);
    }

    private static void tickAutoBlockLogic(EntityLivingBase target) {
        long now = System.currentTimeMillis();
        double targetCycleMs = 1000.0 / Math.max(1.0, Math.min(20.0, aps));

        boolean enemyInIFrame = target.hurtResistantTime > targetHurtThreshold || target.hurtTime > 0;
        boolean playerInIFrame = mode == Mode.Smart && (C.p().hurtTime > playerHurtThreshold || C.p().hurtResistantTime > 10);

        if (mode == Mode.Player_IFrame) {
            if (C.p().hurtTime > playerHurtThreshold || C.p().hurtResistantTime > 10) {
                startBlocking();
            } else {
                stopBlocking();
                PlayerUtil.attack(target);
            }
            return;
        }

        // Enemy_IFrame & Smart
        if (enemyInIFrame || playerInIFrame) {
            startBlocking();
            inAttackBurst = false;
            attacksDoneInCycle = 0;
        } else {
            // Target is damageable (out of hurt-resistance window)
            if (inAttackBurst) {
                stopBlocking();

                // Deliver standard controller attacks
                if (attacksDoneInCycle < attacksPerUnblock) {
                    PlayerUtil.attack(target);
                    attacksDoneInCycle++;
                }

                if (attacksDoneInCycle >= attacksPerUnblock || (now - lastCycleTime >= targetCycleMs / 2.0)) {
                    inAttackBurst = false;
                    attacksDoneInCycle = 0;
                    startBlocking();
                }
            } else {
                boolean timePassed = (now - lastCycleTime) >= targetCycleMs;
                if (timePassed) {
                    lastCycleTime = now;
                    inAttackBurst = true;
                    attacksDoneInCycle = 0;
                    stopBlocking();
                    PlayerUtil.attack(target);
                    attacksDoneInCycle++;
                } else {
                    startBlocking();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (!C.isInGame() || C.p().isDead) {
            stopBlocking();
        }
    }

    @SubscribeEvent
    public static void onRespawn(RespawnEvent event) {
        stopBlocking();
    }

    private static boolean isHoldingSword() {
        ItemStack held = C.p().getHeldItem();
        return held != null && held.getItem() instanceof ItemSword;
    }

    private static EntityLivingBase findTarget() {
        List<EntityLivingBase> targets = TargetUtil.getPossibleTargets(range, throughWalls, false);
        if (targets.isEmpty()) return null;

        targets.sort(Comparator.comparingDouble(TargetUtil::getDistanceToEntity));
        return targets.get(0);
    }

    private static void startBlocking() {
        if (blocking) return;

        if (packetMode) {
            ItemStack held = C.p().getHeldItem();
            if (held != null) {
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(held));
                serverBlocking = true;
            }
        } else {
            KeyBindingBridge.from(C.mc.gameSettings.keyBindUseItem).bridge$setDown(true);
            MinecraftBridge.from(C.mc).bridge$rightClickMouse();
            serverBlocking = true;
        }

        blocking = true;
    }

    public static void stopBlocking() {
        if (!blocking && !serverBlocking) return;

        if (packetMode) {
            PacketUtil.sendPacket(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
            ));
            serverBlocking = false;
        } else {
            KeyBindingBridge.from(C.mc.gameSettings.keyBindUseItem).bridge$setDown(false);
            if (C.mc.playerController != null && C.p() != null) {
                C.mc.playerController.onStoppedUsingItem(C.p());
            }
            serverBlocking = false;
        }

        blocking = false;
    }

    @Override
    protected void onEnable() {
        blocking = false;
        serverBlocking = false;
        currentTarget = null;
        attacksDoneInCycle = 0;
        inAttackBurst = false;
        lastCycleTime = 0;
    }

    @Override
    protected void onDisable() {
        stopBlocking();
    }

    @Override
    public String arrayListExtraInfo() {
        return mode.name() + " (" + (int) aps + " APS)";
    }
}

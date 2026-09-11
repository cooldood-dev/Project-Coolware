package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.minecraft.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;

import java.util.Comparator;
import java.util.List;

@RegisterModule(
        name = "Tick Base",
        description = "Provides Tick Base functionality for the client.",
        category = Category.COMBAT
)
// this is probably not what tickbase is, im probably dumb.
public class TickBase extends Module {
    @RegisterSubModule(name = "Through Walls")
    public static boolean throughWalls = false;
    @RegisterSubModule(name = "Swords Only")
    public static boolean swordsOnly = true;

    @RegisterSubModule(name = "Mode")
    public static Mode mode = Mode.Timer;

    public enum Mode {
        Timer,
        Blink
    }

    @RegisterSubModule(name = "Target Range", min = 1, max = 8, parent = "Mode", modeParentString = {"Timer"})
    public static double targetRange = 4;
    @RegisterSubModule(name = "Max Ticks", min = 2, max = 20, parent = "Mode", modeParentString = {"Timer"})
    public static int maxTicks = 7;

    @RegisterSubModule(name = "Max Blink Ticks", min = 1, max = 20, parent = "Mode", modeParentString = {"Blink"})
    public static int maxBlinkTicks = 10;
    @RegisterSubModule(name = "Start Blink Range", min = 2, max = 8, parent = "Mode", modeParentString = {"Blink"})
    public static double startBlinkRange = 5;
    @RegisterSubModule(name = "End Blink Range", min = 1, max = 8, parent = "Mode", modeParentString = {"Blink"})
    public static double endBlinkRange = 3;

    private static float currentTimer = 1;
    private static int remainingBalance = 0;
    private static boolean balancing = false;

    private static int blinkTicks = -1;

    @SubscribeEvent
    public static void onPlayerUpdate(PlayerUpdateEvent event) {
        if (currentTimer != 1) {
            if (balancing) {
                remainingBalance--;
                if (remainingBalance <= 1) disable();

                return;
            }

            TimerUtil.popTimer(currentTimer);
            currentTimer = remainingBalance;
            TimerUtil.pushTimer(remainingBalance);
            balancing = true;

            return;
        }

        List<EntityLivingBase> targets = TargetUtil.getPossibleTargets(20, throughWalls, true);

        if (targets.isEmpty() || (swordsOnly && !(InventoryUtil.getHeldItem() instanceof ItemSword))) {
            if (blinkTicks != -1) {
                BlinkUtil.popBlink(true, false);
                blinkTicks = -1;
            }
            return;
        }

        targets.sort(Comparator.comparingDouble(TargetUtil::getDistanceToEntity));
        EntityLivingBase closestTarget = targets.get(0);
        boolean movingTowards = PlayerUtil.movingTowardsEntity(closestTarget);

        double distance = TargetUtil.getDistanceToEntity(closestTarget);
        int ticksUntilInRange = PlayerUtil.ticksUntilInRange(closestTarget, targetRange);

        switch (mode) {
            case Blink:
                if (blinkTicks != -1) {
                    if (blinkTicks >= maxBlinkTicks || distance <= endBlinkRange || !movingTowards) {
                        BlinkUtil.popBlink(true, false);
                        blinkTicks = -1;
                        return;
                    }

                    blinkTicks++;
                }
                else if (distance > endBlinkRange && movingTowards && canBlink()) {
                    blinkTicks = 0;
                    BlinkUtil.pushBlink(true, false);
                }

                break;
            case Timer:
                if (movingTowards && ticksUntilInRange > 0 && distance > targetRange && ticksUntilInRange <= maxTicks) {
                    remainingBalance = ticksUntilInRange;
                    currentTimer = 1f/ticksUntilInRange;
                    TimerUtil.pushTimer(currentTimer);
                }
                break;
        }
    }

    private static boolean canBlink() {
        return !KillAura.isBlocking() || KillAura.autoblockMode != KillAura.AutoBlockMode.Watchdog;
    }

    @Override
    protected void onEnable() {

    }

    private static void disable() {
        if (currentTimer != 1) {
            TimerUtil.popTimer(currentTimer);
            currentTimer = 1;
            remainingBalance = 0;
            balancing = false;
        }

        if (blinkTicks != -1) {
            BlinkUtil.popBlink(true, false);
            blinkTicks = -1;
        }
    }

    @Override
    public String arrayListExtraInfo() {
        return (mode == Mode.Blink ? startBlinkRange : maxTicks) + "";
    }

    @Override
    protected void onDisable() {
        disable();
    }
}

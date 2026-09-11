package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.MathUtil;
import com.github.cooldood.utils.minecraft.BlinkUtil;
import com.github.cooldood.utils.minecraft.ChatUtil;
import com.github.cooldood.utils.minecraft.InventoryUtil;
import com.github.cooldood.utils.minecraft.TargetUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;

import java.util.Comparator;
import java.util.List;

@RegisterModule(
        name = "Fake Lag",
        description = "Provides Fake Lag functionality for the client.",
        category = Category.COMBAT
)
public class FakeLag extends Module {
    @RegisterSubModule(name = "Through Walls")
    public static boolean throughWalls = false;

    @RegisterSubModule(name = "Swords Only")
    public static boolean swordsOnly = true;

    @RegisterSubModule(name = "Range", min = 2, max = 8)
    public static double range = 5;

    @RegisterSubModule(name = "Min Pulse Ticks", min = 1, max = 10)
    public static int minPulseTicks = 1;

    @RegisterSubModule(name = "Max Pulse Ticks", min = 1, max = 10)
    public static int maxPulseTicks = 4;

    private static int pulseTicks = -1;

    @SubscribeEvent
    public static void onPlayerUpdate(PlayerUpdateEvent event) {
        List<EntityLivingBase> targets = TargetUtil.getPossibleTargets(range, throughWalls, true);

        if (targets.isEmpty() || (swordsOnly && !(InventoryUtil.getHeldItem() instanceof ItemSword)) || !canBlink()) {
            disable();
            return;
        }

        targets.sort(Comparator.comparingDouble(TargetUtil::getDistanceToEntity));

        EntityLivingBase closestTarget = targets.get(0);
        double distance = TargetUtil.getDistanceToEntity(closestTarget);

        if (distance > range) {
            disable();
            return;
        }

        ChatUtil.chat(pulseTicks);

        if (pulseTicks == -1) {
            pulseTicks = (int) MathUtil.getRandomInRange(minPulseTicks, maxPulseTicks);
            BlinkUtil.pushBlink(true, false);
        }
        else if (pulseTicks == 0) {
            BlinkUtil.popBlink(true, false);
            pulseTicks = -1;
        }
        else {
            pulseTicks--;
        }
    }

    private static boolean canBlink() {
        return !KillAura.isBlocking() || KillAura.autoblockMode != KillAura.AutoBlockMode.Watchdog;
    }

    @Override
    protected void onEnable() {

    }

    private static void disable() {
        if (pulseTicks > 0) {
            BlinkUtil.popBlink(true, false);
            pulseTicks = -1;
        }
    }
    @Override
    protected void onDisable() {
        disable();
    }
}

package com.github.cooldood.modules.impl.player;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.AttackBlockEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.InventoryUtil;
import net.minecraft.network.play.client.C07PacketPlayerDigging;

@RegisterModule(
        name = "Auto Tool",
        description = "Provides Auto Tool functionality for the client.",
        category = Category.PLAYER
)
// todo: dont switch back tick 1
public class AutoTool extends Module {
    @RegisterSubModule(name = "Crouching Only")
    public static boolean onlyWhenCrouching = true;

    private static int currentSlot = -1;

    @SubscribeEvent
    public static void onAttackBlock(AttackBlockEvent event) {
        if (onlyWhenCrouching && !C.p().isSneaking()) return;

        int bestSlot = InventoryUtil.getBestSlotForBlock(event.pos);

        if (bestSlot == C.p().inventory.currentItem) return;

        currentSlot = C.p().inventory.currentItem;
        C.p().inventory.currentItem = InventoryUtil.getBestSlotForBlock(event.pos);
    }

    @SubscribeEvent
    public static void onAttackBlock(PacketEvent.Send event) {
        if (currentSlot == -1) return;
        if (!(event.packet instanceof C07PacketPlayerDigging)) return;

        C07PacketPlayerDigging packet = (C07PacketPlayerDigging) event.packet;
        if (packet.getStatus() != C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK && packet.getStatus() != C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) return;

        C.p().inventory.currentItem = currentSlot;
        currentSlot = -1;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

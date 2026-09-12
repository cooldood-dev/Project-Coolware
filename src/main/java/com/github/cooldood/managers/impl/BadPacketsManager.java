package com.github.cooldood.managers.impl;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PacketEvent;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;

public class BadPacketsManager {

    private static boolean slot = false;
    private static boolean attack = false;
    private static boolean swing = false;
    private static boolean block = false;
    private static boolean inventory = false;

    public static boolean bad() {
        return bad(true, true, true, true, true);
    }

    public static boolean bad(boolean slot, boolean attack, boolean swing, boolean block, boolean inventory) {
        return (BadPacketsManager.slot && slot)
                || (BadPacketsManager.attack && attack)
                || (BadPacketsManager.swing && swing)
                || (BadPacketsManager.block && block)
                || (BadPacketsManager.inventory && inventory);
    }

    public static void reset() {
        slot = false;
        attack = false;
        swing = false;
        block = false;
        inventory = false;
    }

    @SubscribeEvent(priority = 1)
    public static void onPacketSend(PacketEvent.Send event) {
        Packet<?> p = event.packet;
        if (p instanceof C09PacketHeldItemChange) {
            slot = true;
        } else if (p instanceof C0APacketAnimation) {
            swing = true;
        } else if (p instanceof C02PacketUseEntity) {
            attack = true;
        } else if (p instanceof C08PacketPlayerBlockPlacement || p instanceof C07PacketPlayerDigging) {
            block = true;
        } else if (p instanceof C0EPacketClickWindow
                || (p instanceof C16PacketClientStatus && ((C16PacketClientStatus) p).getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT)
                || p instanceof C0DPacketCloseWindow) {
            inventory = true;
        } else if (p instanceof C03PacketPlayer) {
            reset();
        }
    }
}

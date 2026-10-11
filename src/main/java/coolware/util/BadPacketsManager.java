package coolware.util;

import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;

public final class BadPacketsManager {
    private static boolean slot;
    private static boolean attack;
    private static boolean swing;
    private static boolean block;
    private static boolean inventory;

    private BadPacketsManager() {
    }

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

    public static void onPacketSend(Packet<?> packet) {
        if (packet instanceof C09PacketHeldItemChange) {
            slot = true;
        } else if (packet instanceof C0APacketAnimation) {
            swing = true;
        } else if (packet instanceof C02PacketUseEntity) {
            attack = true;
        } else if (packet instanceof C08PacketPlayerBlockPlacement
                || packet instanceof C07PacketPlayerDigging) {
            block = true;
        } else if (packet instanceof C0EPacketClickWindow
                || packet instanceof C0DPacketCloseWindow) {
            inventory = true;
        } else if (packet instanceof C03PacketPlayer) {
            reset();
        }
    }

    public static void reset() {
        slot = false;
        attack = false;
        swing = false;
        block = false;
        inventory = false;
    }
}

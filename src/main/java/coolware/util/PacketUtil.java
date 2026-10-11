package coolware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;

public class PacketUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void sendPacket(Packet<?> packet) {
        if (mc.getNetHandler() == null) return;
        mc.getNetHandler().addToSendQueue(packet);
    }

    public static void sendPacketNoEvent(Packet<?> packet) {
        if (mc.getNetHandler() == null) return;
        mc.getNetHandler().getNetworkManager().sendPacket(packet);
    }
}

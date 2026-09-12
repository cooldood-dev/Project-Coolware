package com.github.cooldood.utils.minecraft;

import com.github.cooldood.utils.client.C;
import net.minecraft.network.Packet;

public class PacketUtils {
    public static void sendPacket(Packet<?> packet) {
        if (C.p() != null && C.p().sendQueue != null) {
            C.p().sendQueue.addToSendQueue(packet);
        }
    }
}

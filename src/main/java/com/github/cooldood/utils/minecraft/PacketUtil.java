package com.github.cooldood.utils.minecraft;

import com.github.cooldood.bridge.net.minecraft.network.NetworkManagerBridge;
import com.github.cooldood.utils.client.C;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;

public class PacketUtil {
    public static void sendPacket(Packet<?> packet) {
        if (C.mc.getNetHandler() == null) return;
        C.mc.getNetHandler().addToSendQueue(packet);
    }

    public static void receivePacket(Packet<?> packet) {
        if (C.mc.getNetHandler() == null) return;
        NetworkManagerBridge.from(C.mc.getNetHandler().getNetworkManager()).bridge$processPacket((Packet<INetHandler>) packet);
    }
}
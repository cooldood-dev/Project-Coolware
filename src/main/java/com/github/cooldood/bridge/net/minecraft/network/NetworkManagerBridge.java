package com.github.cooldood.bridge.net.minecraft.network;

import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;

public interface NetworkManagerBridge {
    static NetworkManagerBridge from(Object instance) {
        return (NetworkManagerBridge) instance;
    }

    void bridge$processPacket(Packet<INetHandler> packet);
}

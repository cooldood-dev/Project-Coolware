package com.github.cooldood.bridge.net.minecraft.client.entity;

import net.minecraft.client.network.NetworkPlayerInfo;

public interface AbstractClientPlayerBridge {
    static AbstractClientPlayerBridge from(Object instance) {
        return (AbstractClientPlayerBridge) instance;
    }

    NetworkPlayerInfo bridge$getPlayerInfo();
}

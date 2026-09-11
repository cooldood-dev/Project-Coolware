package com.github.cooldood.bridge.net.minecraft.client.multiplayer;

public interface PlayerControllerMPBridge {
    static PlayerControllerMPBridge from(Object instance) {
        return (PlayerControllerMPBridge) instance;
    }

    float bridge$getCurBlockDamageMP();
}

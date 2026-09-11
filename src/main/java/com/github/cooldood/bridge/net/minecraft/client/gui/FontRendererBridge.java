package com.github.cooldood.bridge.net.minecraft.client.gui;

public interface FontRendererBridge {
    static FontRendererBridge from(Object instance) {
        return (FontRendererBridge) instance;
    }

    void bridge$resetStyles();
    int bridge$renderString(String p_renderString_1_, float p_renderString_2_, float p_renderString_3_, int p_renderString_4_, boolean p_renderString_5_);
}
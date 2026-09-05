package com.github.cooldood.bridge.net.minecraft.client.model;

import net.minecraft.client.model.TexturedQuad;

public interface ModelBoxBridge {
    static ModelBoxBridge from(Object instance) {
        return (ModelBoxBridge) instance;
    }

    TexturedQuad[] bridge$quadList();
}
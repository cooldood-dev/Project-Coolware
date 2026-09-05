package com.github.cooldood.bridge.net.minecraft.client.model;

public interface ModelRendererBridge {
    static ModelRendererBridge from(Object instance) {
        return (ModelRendererBridge) instance;
    }

    boolean bridge$compiled();
    void bridge$compileDisplayList(float scale);
}
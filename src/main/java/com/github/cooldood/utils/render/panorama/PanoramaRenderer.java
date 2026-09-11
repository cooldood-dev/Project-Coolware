package com.github.cooldood.utils.render.panorama;

import com.github.cooldood.bridge.net.minecraft.client.MinecraftBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

// stole from lifix, shoutout to lesbianhack
public class PanoramaRenderer {
    private static PanoramaRenderer INSTANCE;

    private final Minecraft minecraft;
    private final CubeMap cubeMap;
    private float time;

    public PanoramaRenderer(CubeMap cubeMap) {
        this.cubeMap = cubeMap;
        this.minecraft = Minecraft.getMinecraft();
    }

    public void render(float opacity) {
        float delta = MinecraftBridge.from(this.minecraft).bridge$getTimer().bridge$getTickDelta();
        this.render(delta, opacity);
    }

    public void render(float partialTicks, float opacity) {
        this.time += partialTicks;
        this.cubeMap.render(this.minecraft, MathHelper.sin(this.time * 0.001f) * 5.0f + 25.0f,
                -this.time * 0.1f, opacity);
        this.cubeMap.setupGuiState(Minecraft.isRunningOnMac);
    }

    public static PanoramaRenderer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PanoramaRenderer(new CubeMap(new ResourceLocation("panorama/panorama")));
        }
        return INSTANCE;
    }
}
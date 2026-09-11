package com.github.cooldood.mixins.net.minecraft.client.model;

import com.github.cooldood.bridge.net.minecraft.client.model.ModelRendererBridge;
import net.minecraft.client.model.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ModelRenderer.class)
public abstract class ModelRendererMixin implements ModelRendererBridge {
    @Shadow private boolean compiled;

    @Shadow
    protected abstract void compileDisplayList(float scale);

    @Override
    public boolean bridge$compiled() {
        return this.compiled;
    }

    @Override
    public void bridge$compileDisplayList(float scale) {
        this.compileDisplayList(scale);
    }
}

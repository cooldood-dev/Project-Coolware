package com.github.cooldood.mixins.net.minecraft.client.model;

import com.github.cooldood.bridge.net.minecraft.client.model.ModelBoxBridge;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.TexturedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ModelBox.class)
public class ModelBoxMixin implements ModelBoxBridge {
    @Shadow
    private TexturedQuad[] quadList;

    @Override
    public TexturedQuad[] bridge$quadList() {
        return this.quadList;
    }
}

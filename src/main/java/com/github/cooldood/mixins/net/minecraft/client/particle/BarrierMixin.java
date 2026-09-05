package com.github.cooldood.mixins.net.minecraft.client.particle;

import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.render.RenderBarriers;
import net.minecraft.client.particle.Barrier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Barrier.class)
public class BarrierMixin {
    @Inject(method = "renderParticle", at = @At("HEAD"), cancellable = true)
    public void onRenderParticle(CallbackInfo ci) {
        // barrier "particle" still renders after a barrier is broken, well done minecraft.
        if (ModuleManager.isEnabled(RenderBarriers.class)) ci.cancel();
    }
}

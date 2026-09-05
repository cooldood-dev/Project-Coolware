package com.github.cooldood.mixins.net.minecraft.client.renderer;

import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.render.Freecam;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderGlobal.class)
public class RenderGlobalMixin {
    @Shadow private ViewFrustum viewFrustum;

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewFrustum;updateChunkPositions(DD)V"))
    public void onUpdateChunkPositions(ViewFrustum instance, double viewEntityX, double viewEntityZ) {
        // if freecaming, don't unload chunks, because minecraft won't reload them...
        if (!ModuleManager.isEnabled(Freecam.class)) this.viewFrustum.updateChunkPositions(viewEntityX, viewEntityZ);
    }
}

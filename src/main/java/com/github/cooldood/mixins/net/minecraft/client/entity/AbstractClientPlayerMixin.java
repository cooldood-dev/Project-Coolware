package com.github.cooldood.mixins.net.minecraft.client.entity;

import com.github.cooldood.bridge.net.minecraft.client.entity.AbstractClientPlayerBridge;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.render.NoFOV;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin implements AbstractClientPlayerBridge {
    @Shadow
    protected abstract NetworkPlayerInfo getPlayerInfo();

    @Inject(at = @At("HEAD"), method = "getFovModifier", cancellable = true)
    public void getFovModifier(CallbackInfoReturnable<Float> cir) {
        if (ModuleManager.isEnabled(NoFOV.class)) cir.setReturnValue(1f);
    }

    @Inject(at = @At("HEAD"), method = "getLocationCape", cancellable = true)
    public void getLocationCape(CallbackInfoReturnable<net.minecraft.util.ResourceLocation> cir) {
        if (ModuleManager.isEnabled(com.github.cooldood.modules.impl.render.Capes.class)) {
            net.minecraft.client.entity.AbstractClientPlayer self = (net.minecraft.client.entity.AbstractClientPlayer) (Object) this;
            if (self == com.github.cooldood.utils.client.C.p()) {
                cir.setReturnValue(com.github.cooldood.modules.impl.render.Capes.getCapeResource());
            }
        }
    }

    @Override
    public NetworkPlayerInfo bridge$getPlayerInfo() {
        return this.getPlayerInfo();
    }
}

package com.github.cooldood.mixins.net.minecraft.world;

import com.github.cooldood.events.Bus;
import com.github.cooldood.events.impl.WorldUnloadEvent;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldServer.class)
public class WorldServerMixin {
    @Inject(method = "flush", at = @At(value = "TAIL"))
    private void postWorldUnloadEvent(CallbackInfo ci) {
        Bus.post(new WorldUnloadEvent());
    }
}

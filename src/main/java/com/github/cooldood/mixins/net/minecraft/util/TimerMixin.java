package com.github.cooldood.mixins.net.minecraft.util;

import com.github.cooldood.bridge.net.minecraft.util.TimerBridge;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Timer.class)
public class TimerMixin implements TimerBridge {
    @Shadow public float timerSpeed;
    @Shadow float ticksPerSecond;
    @Unique private float scale$msPerTick = 0f;
    @Unique private float scale$tickDelta = 0f;
    @Unique private long scale$lastMs = 0;

    @Inject(method = "updateTimer", at = @At("HEAD"))
    private void impl$updateTimer(CallbackInfo ci) {
        this.scale$msPerTick = 1000.0f / ticksPerSecond;
        long currentTime = System.currentTimeMillis();
        if (this.scale$lastMs == 0) {
            this.scale$lastMs = currentTime;
        }
        this.scale$tickDelta = (float)(currentTime - this.scale$lastMs) / this.scale$msPerTick;
        this.scale$lastMs = currentTime;
    }

    public float bridge$getTimerSpeed() {
        return this.timerSpeed;
    }

    public void bridge$setTimerSpeed(float timerSpeed) {
        this.timerSpeed = timerSpeed;
    }

    public float bridge$getTickDelta() {
        return this.scale$tickDelta;
    }
}
package com.github.cooldood.mixins.net.minecraft.client.multiplayer;

import com.github.cooldood.bridge.net.minecraft.client.multiplayer.PlayerControllerMPBridge;
import com.github.cooldood.events.Bus;
import com.github.cooldood.events.impl.AttackBlockEvent;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public class PlayerControllerMPMixin implements PlayerControllerMPBridge {
    @Shadow
    private float curBlockDamageMP;

    @Inject(method = "clickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/NetHandlerPlayClient;addToSendQueue(Lnet/minecraft/network/Packet;)V", ordinal = 2))
    private void onClickBlock(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        Bus.post(new AttackBlockEvent(loc));
    }

    @Override
    public float bridge$getCurBlockDamageMP() {
        return this.curBlockDamageMP;
    }
}
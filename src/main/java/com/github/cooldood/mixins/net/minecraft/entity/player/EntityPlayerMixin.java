package com.github.cooldood.mixins.net.minecraft.entity.player;

import com.github.cooldood.modules.impl.movement.KeepSprint;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.PlayerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayer.class)
public class EntityPlayerMixin {

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;isSpectator()Z"))
    public boolean noClip(EntityPlayer instance) {
        return instance.isSpectator() || PlayerUtil.noClip;
    }

    @Inject(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"), cancellable = true)
    public void onSetSprinting(Entity entity, CallbackInfo ci) {
        /*
        if (i > 0) {
            targetEntity.addVelocity((double)(-MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F) * (float)i * 0.5F), 0.1D, (double)(MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F) * (float)i * 0.5F));
            this.motionX *= 0.6D; <- reset this
            this.motionZ *= 0.6D; <- reset this
            this.setSprinting(false); <- cancel this
        }
         */
        if (KeepSprint.shouldKeepMotion()) {
            C.p().motionX /= 0.6D;
            C.p().motionZ /= 0.6D;
        }
        if (KeepSprint.shouldKeepSprint()) {
            ci.cancel();
        }
    }
}

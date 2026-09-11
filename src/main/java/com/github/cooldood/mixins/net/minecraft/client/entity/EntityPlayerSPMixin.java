package com.github.cooldood.mixins.net.minecraft.client.entity;

import com.github.cooldood.events.Bus;
import com.github.cooldood.events.impl.MotionEvent;
import com.github.cooldood.events.impl.MovementInputEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.movement.NoSlow;
import com.github.cooldood.modules.impl.movement.Sneak;
import com.github.cooldood.modules.impl.movement.Sprint;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.KeybindHandler;
import com.github.cooldood.utils.minecraft.PlayerUtil;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovementInput;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class EntityPlayerSPMixin {
    @Shadow
    public abstract void setSprinting(boolean sprinting);

    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true)
    private void onUpdate$pre(CallbackInfo ci) {
        PlayerUtil.fakePlayerPosAndRot();

        PlayerUpdateEvent playerUpdateEvent = new PlayerUpdateEvent();
        Bus.post(playerUpdateEvent);

        if (playerUpdateEvent.isCancelled()) ci.cancel();
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void onUpdate$post(CallbackInfo ci) {
        PlayerUtil.resetFakePlayerPosAndRot();
    }

    // injects after minecraft decides what keys your pressing
    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V", shift = At.Shift.AFTER))
    public void editPlayerMoveInput(CallbackInfo ci) {
        MovementInputEvent movementInputEvent = new MovementInputEvent(C.p().movementInput);
        Bus.post(movementInputEvent);
        C.p().movementInput = movementInputEvent.movementInput;
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isUsingItem()Z"))
    public boolean editIsUsingItem(EntityPlayerSP instance) {
        return NoSlow.shouldSlowDown();
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setSprinting(Z)V"))
    public void setSprinting(EntityPlayerSP instance, boolean sprinting) {
        if (Sprint.shouldOmniSprint()) this.setSprinting(true);
        else this.setSprinting(sprinting);
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInput;sneak:Z"))
    public boolean editPlayerSneaking(MovementInput instance) {
        // the value of this only matters if you're flying
        if (!C.p().onGround) return KeybindHandler.isKeyDown(C.mc.gameSettings.keyBindSneak) && C.mc.currentScreen == null;
        // overwrite isSneaking on ground to make sure sprinting ticks arnt reset!
        return C.p().movementInput.sneak && !ModuleManager.isEnabled(Sneak.class);
    }


    @Inject(method = "onUpdateWalkingPlayer", at = @At(value = "HEAD"))
    public void onUpdateWalkingPlayer(CallbackInfo ci) {
        PlayerUtil.motionEvent = new MotionEvent(
                new Vec3(C.p().posX, C.p().getEntityBoundingBox().minY, C.p().posZ),
                C.p().isSneaking(),
                C.p().isSprinting(),
                C.p().onGround
        );

        Bus.post(PlayerUtil.motionEvent);
    }

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;rotationYaw:F"))
    public float editRotationYaw(EntityPlayerSP instance) {
        return PlayerUtil.currentRotation().yaw;
    }

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;rotationPitch:F"))
    public float editRotationPitch(EntityPlayerSP instance) {
        return PlayerUtil.currentRotation().pitch;
    }

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;posX:D"))
    public double editPosX(EntityPlayerSP instance) {
        return PlayerUtil.motionEvent.pos.xCoord;
    }
    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/util/AxisAlignedBB;minY:D"))
    public double editPosY(AxisAlignedBB instance) {
        return PlayerUtil.motionEvent.pos.yCoord;
    }
    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;posZ:D"))
    public double editPosZ(EntityPlayerSP instance) {
        return PlayerUtil.motionEvent.pos.zCoord;
    }

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isSneaking()Z"))
    public boolean editIsSneaking(EntityPlayerSP instance) {
        return PlayerUtil.motionEvent.sneaking;
    }
    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isSprinting()Z"))
    public boolean editIsSprinting(EntityPlayerSP instance) {
        return PlayerUtil.motionEvent.sprinting;
    }
    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onGround:Z"))
    public boolean editOnGround(EntityPlayerSP instance) {
        return PlayerUtil.motionEvent.onGround;
    }
}

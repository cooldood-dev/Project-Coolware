package com.github.cooldood.mixins.net.minecraft.client.renderer;

import com.github.cooldood.events.Bus;
import com.github.cooldood.events.impl.RenderTickEvent;
import com.github.cooldood.events.impl.RenderWorldEvent;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.client.MovementFix;
import com.github.cooldood.modules.impl.combat.Reach;
import com.github.cooldood.modules.impl.movement.Scaffold;
import com.github.cooldood.modules.impl.render.NoRender;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.PlayerUtil;
import com.github.cooldood.utils.minecraft.RotationUtil;
import com.github.cooldood.utils.minecraft.WorldUtil;
import com.github.cooldood.utils.render.RenderUtil;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderer.class, priority = 1001)
public abstract class EntityRendererMixin {
    @Shadow
    private float fogColorRed;

    @Shadow
    private float fogColorGreen;

    @Shadow
    private float fogColorBlue;

    @Unique private static boolean itemSpoofing = false;
    @Unique private static int itemSpoofOriginalSlot = -1;

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"))
    private void itemSpoofUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo ci) {
        if (C.p() != null && ModuleManager.isEnabled(Scaffold.class) && Scaffold.itemSpoof && Scaffold.getSlot() >= 0) {
            itemSpoofOriginalSlot = C.p().inventory.currentItem;
            C.p().inventory.currentItem = Scaffold.getSlot();
            itemSpoofing = true;
        }
    }

    @Inject(method = "updateCameraAndRender", at = @At("RETURN"))
    private void itemSpoofPostUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo ci) {
        if (itemSpoofing) {
            C.p().inventory.currentItem = itemSpoofOriginalSlot;
            itemSpoofing = false;
        }
    }

    // my only other minecraft mod uses https://github.com/Sunderw3k/InjectAPI which has cuter and mixins <3
    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER))
    private void updateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo ci) {
        RenderUtil.renderSide = RenderUtil.RenderSide.Tick;
        Bus.post(new RenderTickEvent());
    }

    @Inject(method = "renderWorldPass", at = @At(value = "HEAD"))
    private void onRenderWorldPassHEAD(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        RenderUtil.renderSide = RenderUtil.RenderSide.World;
    }

    @Inject(method = "renderWorldPass", at = @At(value = "TAIL"))
    private void onRenderWorldPassTAIL(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        RenderUtil.renderSide = RenderUtil.RenderSide.Else;
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V"))
    private void onRenderWorldPass(Profiler instance, String name, int pass, float partialTicks, long finishTimeNano) {
        if (name.equals("hand")) {
            Bus.post(new RenderWorldEvent(partialTicks));
        }
        instance.endStartSection(name);
    }

    @Redirect(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;extendedReach()Z"))
    public boolean redirectExtendedReach(PlayerControllerMP instance) {
        // if reach is enabled we gotta make sure "flag" is false
        return Reach.shouldOverwriteReach() || instance.getCurrentGameType().isCreative();
    }

    @Redirect(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;getBlockReachDistance()F"))
    public float redirectBlockReachRayTrace(PlayerControllerMP instance) {
        return Reach.shouldOverwriteReach() ? Reach.getBlockReach() : instance.getBlockReachDistance();
    }

    // @ModifyVariable needs to be called before @Redirect, thats a cool fun fact!
    @ModifyVariable(method = "getMouseOver", name = "d1", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getLook(F)Lnet/minecraft/util/Vec3;"))
    public double redirectAttackReach(double value) {
        return Reach.shouldOverwriteReach() ? Reach.getAttackReach() : value;
    }

    @Redirect(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getLook(F)Lnet/minecraft/util/Vec3;"))
    public Vec3 redirectGetLook(Entity instance, float partialTicks) {
        RotationUtil.Rotation prevRot = MovementFix.shouldRotationFix() ? PlayerUtil.lastRotation() : RotationUtil.getPreviousClientRotation();
        RotationUtil.Rotation currentRot = MovementFix.shouldRotationFix() ? PlayerUtil.currentRotation() : RotationUtil.getCurrentClientRotation();

        if (PlayerUtil.shouldFixPlayerFakeLook() && PlayerUtil.realRotation != null) {
            prevRot = PlayerUtil.realRotation;
            currentRot = PlayerUtil.realRotation;
        }

        float f = prevRot.pitch + (currentRot.pitch - prevRot.pitch) * partialTicks;
        float f1 = prevRot.yaw + (currentRot.yaw - prevRot.yaw) * partialTicks;
        return WorldUtil.getVectorForRotation(f, f1);
    }

    @Inject(method = "getMouseOver", at = @At(value = "HEAD"))
    public void onGetMouseOverHEAD(float partialTicks, CallbackInfo ci) {
        // if player rotation or pos is spoofed, we want the real player pos to handle interactions, unless freecam is on
        if (!PlayerUtil.shouldInteractFromFakePos())
            PlayerUtil.fakePlayerPosAndRot();
    }

    @Inject(method = "getMouseOver", at = @At(value = "TAIL"))
    public void onGetMouseOverTAIL(float partialTicks, CallbackInfo ci) {
        if (!PlayerUtil.shouldInteractFromFakePos())
            PlayerUtil.resetFakePlayerPosAndRot();
    }


    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isSpectator()Z"))
    private boolean overrideIsSpectator(EntityPlayerSP instance) {
        return PlayerUtil.noClipRender || C.p().isSpectator();
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void onHurtCameraEffect(float partialTicks, CallbackInfo ci) {
        if (NoRender.noHurtCam()) {
            ci.cancel();
        }
    }

    @Inject(method = "setupViewBobbing", at = @At("HEAD"), cancellable = true)
    private void onSetupViewBobbing(float partialTicks, CallbackInfo ci) {
        if (NoRender.noBobbing()) {
            ci.cancel();
        }
    }

    @Redirect(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z", ordinal = 1))
    private boolean overrideBlindnessOnUpdateFogColor(EntityLivingBase instance, Potion potionIn) {
        return !NoRender.noBlindness() && instance.isPotionActive(potionIn);
    }

    @Inject(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;clearColor(FFFF)V"))
    private void overrideBlindnessOnUpdateFogColor(float partialTicks, CallbackInfo ci) {
        if (NoRender.noBlindness() && NoRender.customBlindnessFog && C.p().isPotionActive(Potion.blindness)) {
            this.fogColorRed = NoRender.blindnessFogColour.getRed() / 255f;
            this.fogColorGreen = NoRender.blindnessFogColour.getGreen() / 255f;
            this.fogColorBlue = NoRender.blindnessFogColour.getBlue() / 255f;
        }
    }

    @Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z", ordinal = 0))
    private boolean overrideBlindnessOnSetupFog(EntityLivingBase instance, Potion potionIn) {
        return (!NoRender.noBlindness() || NoRender.customBlindnessFog) && instance.isPotionActive(potionIn);
    }

    @Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;setFogEnd(F)V", ordinal = 0))
    private void overrideBlindnessOnSetupFogEnd0(float param) {
        GlStateManager.setFogEnd(NoRender.noBlindness() && C.p().isPotionActive(Potion.blindness) ? param * NoRender.fogDistance() : param);
    }

    @Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;setFogStart(F)V", ordinal = 1))
    private void overrideBlindnessOnSetupFogStart(float param) {
        GlStateManager.setFogEnd(NoRender.noBlindness() && C.p().isPotionActive(Potion.blindness) ? param * NoRender.fogDistance() : param);
    }
    @Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;setFogEnd(F)V", ordinal = 1))
    private void overrideBlindnessOnSetupFogEnd1(float param) {
        GlStateManager.setFogEnd(NoRender.noBlindness() && C.p().isPotionActive(Potion.blindness) ? param * NoRender.fogDistance() : param);
    }
}
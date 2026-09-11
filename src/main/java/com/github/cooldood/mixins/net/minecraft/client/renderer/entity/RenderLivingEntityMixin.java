package com.github.cooldood.mixins.net.minecraft.client.renderer.entity;

import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.client.ShowRotations;
import com.github.cooldood.modules.impl.render.ESP;
import com.github.cooldood.modules.impl.render.Nametags;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.PlayerUtil;
import com.github.cooldood.utils.minecraft.TargetUtil;
import com.github.cooldood.utils.render.Render3dUtil;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RendererLivingEntity.class)
public abstract class RenderLivingEntityMixin <T extends EntityLivingBase> extends Render<T> {
    @Shadow
    protected abstract void renderLivingAt(T entityLivingBaseIn, double x, double y, double z);

    protected RenderLivingEntityMixin(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("HEAD"), cancellable = true)
    public void onCheckCullDeadEntities(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (com.github.cooldood.modules.impl.render.NoRender.cullDeadEntities() && entity != null && (entity.isDead || (entity.deathTime > 0 && entity.deathTime >= 15))) {
            ci.cancel();
        }
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;prevRotationPitch:F"))
    public float overridePrevRenderPitch(EntityLivingBase instance) {
        return ShowRotations.getRotation(instance, instance.prevRotationPitch, ShowRotations.RotationPart.PITCH, false);
    }
    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;rotationPitch:F"))
    public float overrideCurrentRenderPitch(EntityLivingBase instance) {
        return ShowRotations.getRotation(instance, instance.rotationPitch, ShowRotations.RotationPart.PITCH, true);
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;prevRotationYawHead:F"))
    public float overridePrevHeadYaw(EntityLivingBase instance) {
        return ShowRotations.getRotation(instance, instance.prevRotationYawHead, ShowRotations.RotationPart.HEAD_YAW, false);
    }
    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;rotationYawHead:F"))
    public float overrideCurrentHeadYaw(EntityLivingBase instance) {
        return ShowRotations.getRotation(instance, instance.rotationYawHead, ShowRotations.RotationPart.HEAD_YAW, true);
    }

    // ik that normally rotations don't look like this, they are slightly smoother, but idc.
    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;prevRenderYawOffset:F"))
    public float overridePrevRenderYaw(EntityLivingBase instance) {
        return ShowRotations.getRotation(instance, instance.prevRenderYawOffset, ShowRotations.RotationPart.BODY_YAW, false);
    }
    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;renderYawOffset:F"))
    public float overrideCurrentRenderYaw(EntityLivingBase instance) {
        return ShowRotations.getRotation(instance, instance.renderYawOffset, ShowRotations.RotationPart.BODY_YAW, true);
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderLivingAt(Lnet/minecraft/entity/EntityLivingBase;DDD)V"))
    public void onRenderLivingAt(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (PlayerUtil.realPos != null && entity == C.p() && !PlayerUtil.isRenderingGuiInventory) {
            Vec3 fakeCameraPos = Render3dUtil.lerpVec(PlayerUtil.prevFakeCameraPos, PlayerUtil.fakeCameraPos, partialTicks);
            Vec3 offsetVec = PlayerUtil.realPos.subtract(fakeCameraPos).subtract(x, y, z);

            this.renderLivingAt(entity, offsetVec.xCoord, offsetVec.yCoord, offsetVec.zCoord);
        }
    }

    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    protected void canRenderName(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (Nametags.shouldHideNametag(entity)) cir.setReturnValue(false);
    }

    @Inject(method = "renderLayers", at = @At("HEAD"))
    protected void renderLayersHEAD(T entitylivingbaseIn, float p_177093_2_, float p_177093_3_, float partialTicks, float p_177093_5_, float p_177093_6_, float p_177093_7_, float p_177093_8_, CallbackInfo ci) {
        if (ESP.chams && ModuleManager.isEnabled(ESP.class)&& TargetUtil.isValidTarget(entitylivingbaseIn, true)) {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1, -1000000);
        }
    }

    @Inject(method = "renderLayers", at = @At("TAIL"))
    protected void renderLayersTAIL(T entitylivingbaseIn, float p_177093_2_, float p_177093_3_, float partialTicks, float p_177093_5_, float p_177093_6_, float p_177093_7_, float p_177093_8_, CallbackInfo ci) {
        if (ESP.chams && ModuleManager.isEnabled(ESP.class) && TargetUtil.isValidTarget(entitylivingbaseIn, true)) {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1, 1000000);
        }
    }


    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return null;
    }
}
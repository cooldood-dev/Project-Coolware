package com.github.cooldood.mixins.net.minecraft.client.model;

import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.render.ESP;
import com.github.cooldood.utils.minecraft.TargetUtil;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPlayer.class)
public class ModelPlayerMixin extends ModelBiped {
    @Shadow public ModelRenderer bipedLeftLegwear;
    @Shadow public ModelRenderer bipedRightLegwear;
    @Shadow public ModelRenderer bipedLeftArmwear;
    @Shadow public ModelRenderer bipedRightArmwear;
    @Shadow public ModelRenderer bipedBodyWear;

    @Inject(method = "render", at = @At("HEAD"))
    public void renderHEAD(Entity entityIn, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float scale, CallbackInfo ci) {
        if (!ModuleManager.isEnabled(ESP.class) || !TargetUtil.isValidTarget(entityIn, true)) return;

        if (ESP.mode == ESP.ESPMode.Outline) {
            GlStateManager.pushMatrix();

            if (this.isChild) {
                float f = 2.0F;
                GlStateManager.scale(1.0F / f, 1.0F / f, 1.0F / f);
                GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
            }

            if (entityIn.isSneaking()) {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }

            ESP.renderOutline(this.bipedHeadwear, scale);
            ESP.renderOutline(this.bipedLeftLegwear, scale);
            ESP.renderOutline(this.bipedRightLegwear, scale);
            ESP.renderOutline(this.bipedLeftArmwear, scale);
            ESP.renderOutline(this.bipedRightArmwear, scale);
            ESP.renderOutline(this.bipedBodyWear, scale);

            GlStateManager.popMatrix();
        }

        if (ESP.chams) {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1, -1000000);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void renderTAIL(Entity entityIn, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float scale, CallbackInfo ci) {
        if (ESP.chams && TargetUtil.isValidTarget(entityIn, true)) {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1, 1000000);
        }
    }
}

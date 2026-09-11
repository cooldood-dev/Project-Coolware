package com.github.cooldood.mixins.net.minecraft.client.renderer.entity;

import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.render.ItemPhysics;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(RenderEntityItem.class)
public abstract class RenderEntityItemMixin extends Render<EntityItem> {

    @Shadow @Final private RenderItem itemRenderer;
    @Shadow protected abstract int func_177078_a(ItemStack stack);

    protected RenderEntityItemMixin(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At("HEAD"), cancellable = true)
    public void onCheckCullItem(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (com.github.cooldood.modules.impl.render.NoRender.cullGroundItems()) {
            ci.cancel();
        }
    }

    /**
     * @author ScaleHack
     * @reason ItemPhysics implementation
     */
    @Overwrite
    private int func_177077_a(EntityItem itemIn, double p_177077_2_, double p_177077_4_, double p_177077_6_, float p_177077_8_, IBakedModel p_177077_9_) {
        Minecraft mc = Minecraft.getMinecraft();
        boolean itemPhysics = ModuleManager.isEnabled(ItemPhysics.class);

        ItemStack itemstack = itemIn.getEntityItem();
        Item item = itemstack.getItem();

        if (item == null) {
            return 0;
        } else {
            boolean flag = p_177077_9_.isGui3d();
            int i = this.func_177078_a(itemstack);
            float f1 = itemPhysics ? -0.13f : MathHelper.sin(((float) itemIn.getAge() + p_177077_8_) / 10.0F + itemIn.hoverStart) * 0.1F + 0.1F;
            float f2 = p_177077_9_.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;
            GlStateManager.translate((float) p_177077_2_, (float) p_177077_4_ + f1 + 0.25F * f2, (float) p_177077_6_);

            if (flag || this.renderManager.options != null) {
                float f3 = (((float) itemIn.getAge() + p_177077_8_) / 20.0F + itemIn.hoverStart) * (180F / (float) Math.PI);
                if (itemPhysics) {
                    if (itemIn.onGround) {
                        double var = ((itemIn.posX + (itemIn.motionX * com.github.cooldood.utils.minecraft.TimerUtil.getTickDelta())) * 200) + ((itemIn.posZ + (itemIn.motionZ * com.github.cooldood.utils.minecraft.TimerUtil.getTickDelta())) * 200);
                        GlStateManager.rotate((float) var, 0f, 1f, 0f);

                        if (item instanceof ItemBlock && ((ItemBlock) item).getBlock() instanceof BlockTrapDoor) {
                            GlStateManager.rotate(0, 1f, 0f, 0f);
                        } else if (!(item instanceof ItemBlock && !((ItemBlock) item).getBlock().isPassable(mc.theWorld, mc.thePlayer.getPosition()))) {
                            GlStateManager.rotate(90, 1f, 0f, 0f);
                        }
                    } else {
                        double x = (itemIn.posX + (itemIn.motionX * com.github.cooldood.utils.minecraft.TimerUtil.getTickDelta())) * 200;
                        double y = (itemIn.posY + (itemIn.motionY * com.github.cooldood.utils.minecraft.TimerUtil.getTickDelta())) * 200;
                        double z = (itemIn.posZ + (itemIn.motionZ * com.github.cooldood.utils.minecraft.TimerUtil.getTickDelta())) * 200;
                        GlStateManager.rotate((float) x, 1f, 0f, 0f);
                        GlStateManager.rotate((float) y, 0f, 1f, 0f);
                        GlStateManager.rotate((float) z, 0f, 0f, 1f);
                    }
                } else {
                    GlStateManager.rotate(f3, 0.0F, 1.0F, 0.0F);
                }
            }

            if (!flag) {
                float f6 = -0.0F * (float) (i - 1) * 0.5F;
                float f4 = -0.0F * (float) (i - 1) * 0.5F;
                float f5 = -0.046875F * (float) (i - 1) * 0.5F;
                GlStateManager.translate(f6, f4, f5);
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            return i;
        }
    }
}

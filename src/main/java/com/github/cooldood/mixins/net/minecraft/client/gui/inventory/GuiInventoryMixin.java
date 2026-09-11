package com.github.cooldood.mixins.net.minecraft.client.gui.inventory;

import com.github.cooldood.utils.minecraft.PlayerUtil;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiInventory.class)
public class GuiInventoryMixin {
    @Inject(method = "drawEntityOnScreen", at = @At("HEAD"))
    private static void drawScreenHEAD(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent, CallbackInfo ci) {
        PlayerUtil.isRenderingGuiInventory = true;
    }

    @Inject(method = "drawEntityOnScreen", at = @At("TAIL"))
    private static void drawScreenTAIL(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent, CallbackInfo ci) {
        PlayerUtil.isRenderingGuiInventory = false;
    }
}

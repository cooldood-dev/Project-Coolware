package com.github.cooldood.mixins.net.minecraft.client.gui;

import com.github.cooldood.events.Bus;
import com.github.cooldood.events.impl.DrawScreenEvent;
import com.github.cooldood.screens.MainMenuScreen;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public class GuiScreenMixin {
    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void onDrawScreenHEAD(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        RenderUtil.renderSide = RenderUtil.RenderSide.GUI;
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void onDrawScreenTAIL(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Bus.post(new DrawScreenEvent());
    }

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void impl$drawBackground(int tint, CallbackInfo ci) {
        // Only draw the custom dark gradient main menu background if the player is NOT in a world
        if (C.w() == null) {
            ci.cancel();
            MainMenuScreen.drawBackground();
        }
    }
}

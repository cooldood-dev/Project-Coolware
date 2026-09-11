package com.github.cooldood.mixins.net.minecraft.client.gui;

import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.client.CoolMainMenu;
import com.github.cooldood.screens.MainMenuScreen;
import com.github.cooldood.utils.client.C;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class GuiMainMenuMixin extends GuiScreen {
    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGui(CallbackInfo ci) {
        if (ModuleManager.isEnabled(CoolMainMenu.class)) C.mc.displayGuiScreen(new MainMenuScreen());
    }
}

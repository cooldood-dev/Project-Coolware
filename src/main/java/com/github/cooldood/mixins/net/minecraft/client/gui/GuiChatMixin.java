package com.github.cooldood.mixins.net.minecraft.client.gui;

import com.github.cooldood.commands.CommandManager;
import com.github.cooldood.utils.render.draggable.DraggableRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public class GuiChatMixin {
    @Shadow
    protected GuiTextField inputField;

    @Inject(method = "autocompletePlayerNames", at = @At("HEAD"), cancellable = true)
    public void autocompleteCommands(CallbackInfo ci) {
        if (!CommandManager.isMessageCommand(this.inputField.getText())) return;

        ci.cancel();
        CommandManager.handleCommandTabbed(this.inputField);
    }

    @Inject(method = "initGui", at = @At("HEAD"))
    public void initGui(CallbackInfo ci) {
        CommandManager.resetTabProgress();
    }

    @Inject(method = "onGuiClosed", at = @At("TAIL"))
    public void onGuiClosed(CallbackInfo ci) {
        DraggableRenderer.saveDraggables();
    }
}

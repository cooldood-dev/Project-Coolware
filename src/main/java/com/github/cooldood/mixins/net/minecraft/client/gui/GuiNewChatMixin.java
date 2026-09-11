package com.github.cooldood.mixins.net.minecraft.client.gui;

import com.github.cooldood.modules.impl.render.ChatFixer;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiNewChat.class)
public class GuiNewChatMixin {
    @Inject(method = "clearChatMessages", at = @At("HEAD"), cancellable = true)
    public void clearChatMessages(CallbackInfo ci) {
        if (!ChatFixer.shouldClearChat()) ci.cancel();
    }

    @Redirect(method = "setChatLine", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0))
    public int redirectDrawnChatLinesSize(List<ChatLine> instance) {
        return instance.size() + 100 - ChatFixer.getMaxChatHistory();
    }

    @Redirect(method = "setChatLine", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 2))
    public int redirectChatLinesSize(List<ChatLine> instance) {
        return instance.size() + 100 - ChatFixer.getMaxChatHistory();
    }
}

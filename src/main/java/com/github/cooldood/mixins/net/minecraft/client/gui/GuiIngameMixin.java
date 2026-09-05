package com.github.cooldood.mixins.net.minecraft.client.gui;

import com.github.cooldood.events.Bus;
import com.github.cooldood.events.impl.RenderScoreboardEvent;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.impl.movement.Scaffold;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class GuiIngameMixin {
    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    protected void renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (Bus.post(new RenderScoreboardEvent(objective))) ci.cancel();
    }

    @Redirect(method = "updateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack itemSpoofGetCurrentItem(InventoryPlayer instance) {
        if (ModuleManager.isEnabled(Scaffold.class) && Scaffold.itemSpoof && Scaffold.getSlot() >= 0) {
            return instance.getStackInSlot(Scaffold.getSlot());
        }
        return instance.getCurrentItem();
    }
}

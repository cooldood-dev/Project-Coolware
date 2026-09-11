package com.github.cooldood.mixins.net.minecraftforge.client;

import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.utils.render.FontUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraftforge.client.GuiIngameForge;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.*;

@Mixin(GuiIngameForge.class)
public class GuiIngameForgeMixin extends GuiIngame {
    public GuiIngameForgeMixin(Minecraft mcIn) {
        super(mcIn);
    }

    @Redirect(method = "renderTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;FFIZ)I"))
    private int onDrawString(FontRenderer instance, String text, float x, float y, int colourInt, boolean dropShadow) {
        if (ThemeModule.shouldUseCustomFont()) {
            boolean isTitle = text.equals(this.displayedTitle);
            int size = isTitle ? 40 : 20;
            double sizeDivisor = isTitle ? 0.25 : 0.5;
            GL11.glScaled(sizeDivisor, sizeDivisor, sizeDivisor);
            return (int) FontUtil.drawString(text, -FontUtil.getStringWidth(text, size) / 2f, y - ((size - 7)*2) + 30, size, new Color(colourInt, true), dropShadow);
        }
        else {
            return instance.drawString(text, x, y, colourInt, dropShadow);
        }
    }
}

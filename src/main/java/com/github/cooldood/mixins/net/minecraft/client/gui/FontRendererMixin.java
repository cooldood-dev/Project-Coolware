package com.github.cooldood.mixins.net.minecraft.client.gui;

import com.github.cooldood.bridge.net.minecraft.client.gui.FontRendererBridge;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.modules.impl.render.NickHider;
import com.github.cooldood.utils.render.FontUtil;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

// fun fact: minecraft feeds the setColor(r,g,b,a) function r,b,g,a in renderString
// its misnamed, this.blue = (float)(p_renderString_4_ >> 8 & 255) / 255.0F; which is the green part.

@Mixin(FontRenderer.class)
public abstract class FontRendererMixin implements FontRendererBridge {
    @Shadow public abstract int getStringWidth(final String p0);
    @Shadow public abstract int drawString(String text, int x, int y, int color);
    @Shadow public abstract int drawString(String text, float x, float y, int color, boolean dropShadow);
    @Shadow private boolean bidiFlag;
    @Shadow protected abstract String bidiReorder(String p_bidiReorder_1_);
    @Shadow protected abstract void resetStyles();
    @Shadow protected abstract int renderString(String p_renderString_1_, float p_renderString_2_, float p_renderString_3_, int p_renderString_4_, boolean p_renderString_5_);

    @Override
    public void bridge$resetStyles() {
        this.resetStyles();
    }

    @Override
    public int bridge$renderString(String p_renderString_1_, float p_renderString_2_, float p_renderString_3_, int p_renderString_4_, boolean p_renderString_5_) {
        return this.renderString(p_renderString_1_, p_renderString_2_, p_renderString_3_, p_renderString_4_, p_renderString_5_);
    }

    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;resetStyles()V", shift = At.Shift.AFTER), cancellable = true)
    public void onDrawString(String text, float x, float y, int colourInt, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        if (text == null || text.isEmpty()) return;

        // fix text to hide disgusting words.
        String text2 = NickHider.fixText(text);

        if (text2.isEmpty()) {
            cir.setReturnValue(0);
            return;
        }

        if (ThemeModule.shouldUseCustomFont()) {
            if (this.bidiFlag) {
                text2 = this.bidiReorder(text);
            }

            if ((colourInt & -67108864) == 0) {
                colourInt |= -16777216;
            }

            // i dont know why the scoreboard always has the colour 20FFFFFF, which is transparent.
            Color colour = new Color(colourInt, colourInt != 0x20FFFFFF);

            cir.setReturnValue((int) (x + (int) FontUtil.drawString(text2, x, y - (ThemeModule.minecraftFontSize - 7), ThemeModule.minecraftFontSize, colour, dropShadow)));
        }
        else if (!text.equals(text2)) {
            cir.setReturnValue(this.drawString(text2, x, y, colourInt, dropShadow));
        }
    }

    @Inject(method = "getStringWidth", at = @At("RETURN"), cancellable = true)
    private void impl$getStringWidth(String text, CallbackInfoReturnable<Integer> cir) {
        String text2 = NickHider.fixText(text);

        if (ThemeModule.shouldUseCustomFont()) cir.setReturnValue(FontUtil.getStringWidth(text2, ThemeModule.minecraftFontSize));
        else if (!text.equals(text2)) cir.setReturnValue(this.getStringWidth(text2));
    }
}
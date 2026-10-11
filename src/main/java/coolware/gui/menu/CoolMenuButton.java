package coolware.gui.menu;

import coolware.font.FontManager;
import coolware.module.visuals.Theme;
import coolware.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

public class CoolMenuButton extends GuiButton {
    private float hoverAnimation = 0.0F;

    public CoolMenuButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        // Smooth hover transition
        if (this.hovered) {
            hoverAnimation = Math.min(1.0F, hoverAnimation + 0.12F);
        } else {
            hoverAnimation = Math.max(0.0F, hoverAnimation - 0.12F);
        }

        Theme theme = Theme.getInstance();
        int accentColor = theme.getColor(0);

        float radius = this.height / 2.0F;

        // 1. Frosted Glassmorphic Pill Background
        int topAlpha = (int) (40 + hoverAnimation * 35);
        int botAlpha = (int) (140 + hoverAnimation * 60);

        int topGlass = (topAlpha << 24) | 0xFFFFFF; // Subtle specular top light
        int botGlass = (botAlpha << 24) | 0x1A0E14; // Deep frosted ruby/dark glass

        RenderUtil.drawRoundedGradient(this.xPosition, this.yPosition, this.width, this.height, radius, topGlass, botGlass);

        // 2. Glowing Rounded Outline (Pill Border)
        int borderAlpha = (int) (100 + hoverAnimation * 155);
        int rBorder = (int) (180 + hoverAnimation * 75);
        int gBorder = (int) (40 + hoverAnimation * 40);
        int bBorder = (int) (60 + hoverAnimation * 60);
        int borderColor = (borderAlpha << 24) | (rBorder << 16) | (gBorder << 8) | bBorder;

        RenderUtil.drawRoundedOutline(this.xPosition, this.yPosition, this.width, this.height, radius, 1.2F, borderColor);

        // 3. Hover Accent Glow Rim
        if (hoverAnimation > 0.01F) {
            int glowAlpha = (int) (hoverAnimation * 180);
            int glowColor = (glowAlpha << 24) | (accentColor & 0xFFFFFF);
            RenderUtil.drawRoundedOutline(this.xPosition + 0.5F, this.yPosition + 0.5F, this.width - 1.0F, this.height - 1.0F, radius - 0.5F, 1.5F, glowColor);
        }

        // 4. Centered SF Typography
        int textColor = this.hovered ? (0xFF000000 | accentColor) : 0xFFF0F0F0;

        if (FontManager.sfMedium18 != null) {
            FontManager.sfMedium18.drawCenteredStringWithShadow(this.displayString, this.xPosition + this.width / 2.0F, this.yPosition + (this.height - FontManager.sfMedium18.getHeight()) / 2.0F + 1, textColor);
        } else {
            FontRenderer fontrenderer = mc.fontRendererObj;
            this.drawCenteredString(fontrenderer, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, textColor);
        }
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }
}

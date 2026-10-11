package coolware.gui.menu;

import coolware.font.FontManager;
import coolware.module.visuals.Theme;
import coolware.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

public class ModernIconButton extends GuiButton {
    public enum ActionType {
        SETTINGS,
        PLAY,
        EXIT,
        GENERIC
    }

    private final ActionType actionType;
    private final String iconGlyph;
    private final String tooltip;
    private final boolean isPrimary;

    private float hoverAnimation = 0.0F;

    public ModernIconButton(int buttonId, int x, int y, int size, ActionType actionType, String iconGlyph, String tooltip, boolean isPrimary) {
        super(buttonId, x, y, size, size, "");
        this.actionType = actionType;
        this.iconGlyph = iconGlyph;
        this.tooltip = tooltip;
        this.isPrimary = isPrimary;
    }

    public ModernIconButton(int buttonId, int x, int y, int size, ActionType actionType, String tooltip, boolean isPrimary) {
        this(buttonId, x, y, size, actionType, "", tooltip, isPrimary);
    }

    public ModernIconButton(int buttonId, int x, int y, int size, String iconGlyph, String tooltip) {
        this(buttonId, x, y, size, ActionType.GENERIC, iconGlyph, tooltip, false);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        if (this.hovered) {
            hoverAnimation = Math.min(1.0F, hoverAnimation + 0.12F);
        } else {
            hoverAnimation = Math.max(0.0F, hoverAnimation - 0.12F);
        }

        Theme theme = Theme.getInstance();
        int accentColor = theme.getColor(0);
        int brightRed = 0xFFFF4054;

        float radius = this.height / 2.0F;
        float x = this.xPosition;
        float y = this.yPosition;
        float s = this.width;
        float cx = x + s / 2.0F;
        float cy = y + s / 2.0F;

        // 1. Red circular glow for primary button or on hover
        if (this.isPrimary || hoverAnimation > 0.01F) {
            float glowMultiplier = this.isPrimary ? (0.65F + hoverAnimation * 0.35F) : hoverAnimation;
            int glowAlpha = (int) (glowMultiplier * 90.0F);
            int glowColor = (glowAlpha << 24) | (accentColor & 0xFFFFFF);
            RenderUtil.drawCircleOutline(cx, cy, radius + 2.0F, 2.0F, glowColor);
        }

        // 2. Glassmorphic Circular Background
        int bgAlpha = (int) ((this.isPrimary ? 85 : 38) + hoverAnimation * 45);
        int topGlass = (bgAlpha << 24) | (this.isPrimary ? 0x2E121E : 0x1E141E);
        int botGlass = (bgAlpha + 40 << 24) | (this.isPrimary ? 0x1E0914 : 0x0D090E);
        RenderUtil.drawRoundedGradient(x, y, s, s, radius, topGlass, botGlass);

        // 3. Border
        int borderAlpha = (int) ((this.isPrimary ? 170 : 45) + hoverAnimation * 180);
        int borderColor = (borderAlpha << 24) | (this.isPrimary || this.hovered ? (accentColor & 0xFFFFFF) : 0xAAAAAA);
        RenderUtil.drawCircleOutline(cx, cy, radius, this.isPrimary ? 1.6F : 1.2F, borderColor);

        // 4. Centered Clear & Bold Vector Icon
        int iconColor = this.hovered ? brightRed : (this.isPrimary ? 0xFFFFFFFF : 0xFFF2EEF2);

        if (this.actionType == ActionType.SETTINGS) {
            RenderUtil.drawGearIcon(cx, cy, 8.2F, iconColor);
        } else if (this.actionType == ActionType.PLAY) {
            RenderUtil.drawPlayTriangle(cx + 1.0F, cy, 12.0F, iconColor);
        } else if (this.actionType == ActionType.EXIT) {
            RenderUtil.drawPowerIcon(cx, cy, 7.2F, 2.0F, iconColor);
        } else {
            if (this.iconGlyph != null && FontManager.faRegular18 != null) {
                FontManager.faRegular18.drawCenteredString(this.iconGlyph, cx, cy - FontManager.faRegular18.getHeight() / 2.0F + 1, iconColor);
            } else {
                RenderUtil.drawPlayTriangle(cx, cy, 9.0F, iconColor);
            }
        }
    }

    public void drawTooltip(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible || !this.hovered || this.tooltip == null || this.tooltip.isEmpty()) return;

        Theme theme = Theme.getInstance();
        int accentColor = theme.getColor(0);

        int textWidth = FontManager.sfMedium14 != null ? FontManager.sfMedium14.getStringWidth(this.tooltip) : (FontManager.sfSemiBold14 != null ? FontManager.sfSemiBold14.getStringWidth(this.tooltip) : mc.fontRendererObj.getStringWidth(this.tooltip));
        float pad = 8.0F;
        float tipW = textWidth + pad * 2.0F;
        float tipH = 20.0F;
        float tipX = this.xPosition + (this.width - tipW) / 2.0F;
        float tipY = this.yPosition - tipH - 8.0F;

        // Tooltip container
        RenderUtil.drawRoundedGradient(tipX, tipY, tipW, tipH, 6.0F, 0xEE1E121A, 0xF80A060C);
        RenderUtil.drawRoundedOutline(tipX, tipY, tipW, tipH, 6.0F, 1.0F, 0xAA000000 | (accentColor & 0xFFFFFF));

        // Tooltip text
        if (FontManager.sfMedium14 != null) {
            FontManager.sfMedium14.drawCenteredString(this.tooltip, tipX + tipW / 2.0F, tipY + (tipH - FontManager.sfMedium14.getHeight()) / 2.0F + 1, 0xFFFFFFFF);
        } else if (FontManager.sfSemiBold14 != null) {
            FontManager.sfSemiBold14.drawCenteredString(this.tooltip, tipX + tipW / 2.0F, tipY + (tipH - FontManager.sfSemiBold14.getHeight()) / 2.0F + 1, 0xFFFFFFFF);
        } else {
            mc.fontRendererObj.drawStringWithShadow(this.tooltip, tipX + pad, tipY + (tipH - 8) / 2.0F, 0xFFFFFFFF);
        }
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }
}

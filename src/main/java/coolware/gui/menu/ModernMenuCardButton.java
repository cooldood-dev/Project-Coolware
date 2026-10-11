package coolware.gui.menu;

import coolware.font.FontManager;
import coolware.module.visuals.Theme;
import coolware.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

public class ModernMenuCardButton extends GuiButton {
    public enum CardType {
        SINGLEPLAYER,
        MULTIPLAYER,
        ALT_MANAGER,
        GENERIC
    }

    private final CardType cardType;
    private final String iconGlyph;
    private final String title;
    private final String subtitle;

    private float hoverAnimation = 0.0F;

    public ModernMenuCardButton(int buttonId, int x, int y, int widthIn, int heightIn, CardType cardType, String iconGlyph, String title, String subtitle) {
        super(buttonId, x, y, widthIn, heightIn, title);
        this.cardType = cardType;
        this.iconGlyph = iconGlyph;
        this.title = title;
        this.subtitle = subtitle;
    }

    public ModernMenuCardButton(int buttonId, int x, int y, int widthIn, int heightIn, String iconGlyph, String title, String subtitle) {
        this(buttonId, x, y, widthIn, heightIn, CardType.GENERIC, iconGlyph, title, subtitle);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        // Smooth 120-180ms hover animation
        if (this.hovered) {
            hoverAnimation = Math.min(1.0F, hoverAnimation + 0.12F);
        } else {
            hoverAnimation = Math.max(0.0F, hoverAnimation - 0.12F);
        }

        Theme theme = Theme.getInstance();
        int accentColor = theme.getColor(0);
        int brightRed = 0xFFFF4054;

        float radius = 10.0F;
        float x = this.xPosition;
        float y = this.yPosition;
        float w = this.width;
        float h = this.height;

        // 1. Subtle Red Ambient Glow behind card on hover
        if (hoverAnimation > 0.01F) {
            int glowAlpha = (int) (hoverAnimation * 42.0F);
            int glowColor = (glowAlpha << 24) | (accentColor & 0xFFFFFF);
            RenderUtil.drawRoundedOutline(x - 1.5F, y - 1.5F, w + 3.0F, h + 3.0F, radius + 1.5F, 2.0F, glowColor);
        }

        // 2. Translucent Glassmorphic Body
        int topAlpha = (int) (18 + hoverAnimation * 28);
        int botAlpha = (int) (48 + hoverAnimation * 45);
        int topGlass = (topAlpha << 24) | 0xFFFFFF;
        int botGlass = (botAlpha << 24) | 0x1A0912;
        RenderUtil.drawRoundedGradient(x, y, w, h, radius, topGlass, botGlass);

        // 3. Border (rgba(255,255,255,0.08) -> Red on hover)
        int borderAlpha = (int) (28 + hoverAnimation * 210);
        int borderR = (int) (255 * (1.0F - hoverAnimation) + ((accentColor >> 16 & 255) * hoverAnimation));
        int borderG = (int) (255 * (1.0F - hoverAnimation) + ((accentColor >> 8 & 255) * hoverAnimation));
        int borderB = (int) (255 * (1.0F - hoverAnimation) + ((accentColor & 255) * hoverAnimation));
        int borderColor = (borderAlpha << 24) | (borderR << 16) | (borderG << 8) | borderB;
        RenderUtil.drawRoundedOutline(x, y, w, h, radius, 1.3F, borderColor);

        // 4. Icon Container (Left side)
        float iconBoxSize = h - 14.0F;
        float iconBoxX = x + 12.0F;
        float iconBoxY = y + 7.0F;

        int iconBoxBgAlpha = (int) (22 + hoverAnimation * 55);
        int iconBoxBg = (iconBoxBgAlpha << 24) | (this.hovered ? (accentColor & 0x00FFFFFF) : 0x00FFFFFF);
        RenderUtil.drawRoundedRect(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 8.0F, iconBoxBg);

        int iconBoxBorderAlpha = (int) (35 + hoverAnimation * 160);
        int iconBoxBorder = (iconBoxBorderAlpha << 24) | (this.hovered ? (accentColor & 0xFFFFFF) : 0xAAAAAA);
        RenderUtil.drawRoundedOutline(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 8.0F, 1.2F, iconBoxBorder);

        // 5. Draw Icon inside container (Clear & Bold Vector geometry)
        int iconColor = this.hovered ? brightRed : 0xFFFFFFFF;
        float iconCenterX = iconBoxX + iconBoxSize / 2.0F;
        float iconCenterY = iconBoxY + iconBoxSize / 2.0F;

        if (this.cardType == CardType.SINGLEPLAYER) {
            RenderUtil.drawGamepadIcon(iconCenterX, iconCenterY, 22.0F, iconColor);
        } else if (this.cardType == CardType.MULTIPLAYER) {
            RenderUtil.drawUsersIcon(iconCenterX, iconCenterY, 22.0F, iconColor);
        } else if (this.cardType == CardType.ALT_MANAGER) {
            RenderUtil.drawIdBadgeIcon(iconCenterX, iconCenterY, 21.0F, iconColor);
        } else {
            if (this.iconGlyph != null && FontManager.faRegular18 != null) {
                FontManager.faRegular18.drawCenteredString(this.iconGlyph, iconCenterX, iconCenterY - FontManager.faRegular18.getHeight() / 2.0F + 1, iconColor);
            } else {
                RenderUtil.drawPlayTriangle(iconCenterX, iconCenterY, 15.0F, iconColor);
            }
        }

        // 6. Title and Subtitle Typography
        float textStartX = iconBoxX + iconBoxSize + 14.0F;
        int titleColor = this.hovered ? 0xFFFFFFFF : 0xFFF2EEF2;
        int subColor = this.hovered ? 0xFFC0B8C4 : 0xFF8A838C;

        if (this.subtitle != null && !this.subtitle.isEmpty()) {
            float titleY = y + 10.0F;
            float subY = y + h - 18.0F;

            if (FontManager.sfBold16 != null) {
                FontManager.sfBold16.drawStringWithShadow(this.title, textStartX, titleY, titleColor);
            } else if (FontManager.sfSemiBold16 != null) {
                FontManager.sfSemiBold16.drawStringWithShadow(this.title, textStartX, titleY, titleColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow("\u00a7l" + this.title, textStartX, titleY, titleColor);
            }

            if (FontManager.sfRegular14 != null) {
                FontManager.sfRegular14.drawStringWithShadow(this.subtitle, textStartX, subY, subColor);
            } else if (FontManager.sfMedium14 != null) {
                FontManager.sfMedium14.drawStringWithShadow(this.subtitle, textStartX, subY, subColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(this.subtitle, textStartX, subY, subColor);
            }
        } else {
            if (FontManager.sfBold16 != null) {
                FontManager.sfBold16.drawStringWithShadow(this.title, textStartX, y + (h - FontManager.sfBold16.getHeight()) / 2.0F + 1, titleColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(this.title, textStartX, y + (h - 8) / 2.0F, titleColor);
            }
        }

        // 7. Right Chevron Vector Icon (Clear, bold, turns red on hover)
        float chevronX = x + w - 20.0F;
        float chevronY = y + h / 2.0F;
        int chevAlpha = (int) (80 + hoverAnimation * 175);
        int chevColor = (chevAlpha << 24) | (this.hovered ? (brightRed & 0xFFFFFF) : 0xFFFFFF);
        RenderUtil.drawChevronRight(chevronX, chevronY, 12.0F, 2.2F, chevColor);
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }
}

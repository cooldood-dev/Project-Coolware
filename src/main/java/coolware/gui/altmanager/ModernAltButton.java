package coolware.gui.altmanager;

import coolware.font.FontManager;
import coolware.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

public class ModernAltButton extends GuiButton {
    public enum Style {
        NORMAL,
        TAB,
        DESTRUCTIVE,
        PRIMARY
    }

    public enum IconType {
        NONE,
        USER,           // for Cracked (user.svg)
        CLIPBOARD_USER, // for Session Token (clipboard-user.svg)
        REFRESH,        // for Refresh Token (refresh.svg)
        MICROSOFT,      // for Microsoft (microsoft.svg)
        COOKIE_BITE,    // for Cookie Login (cookie-bite.svg)
        CLIPBOARD,
        RANDOM,
        DIRECT,
        DELETE,
        COOKIE,
        PLUS
    }

    private final Style style;
    private IconType iconType = IconType.NONE;
    private String iconGlyph = null;
    private boolean active = false;
    private float hoverProgress = 0.0F;

    public ModernAltButton(int buttonId, int x, int y, int widthIn, int heightIn, IconType iconType, String buttonText, Style style) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.iconType = iconType;
        this.style = style;
    }

    public ModernAltButton(int buttonId, int x, int y, int widthIn, int heightIn, String iconGlyph, String buttonText, Style style) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.iconGlyph = iconGlyph;
        this.style = style;
        if (buttonText.toLowerCase().contains("microsoft")) {
            this.iconType = IconType.MICROSOFT;
        } else if (buttonText.toLowerCase().contains("session")) {
            this.iconType = IconType.CLIPBOARD_USER;
        } else if (buttonText.toLowerCase().contains("refresh")) {
            this.iconType = IconType.REFRESH;
        } else if (buttonText.toLowerCase().contains("cracked")) {
            this.iconType = IconType.USER;
        } else if (buttonText.toLowerCase().contains("cookie")) {
            this.iconType = IconType.COOKIE_BITE;
        } else if (buttonText.toLowerCase().contains("clipboard")) {
            this.iconType = IconType.CLIPBOARD;
        } else if (buttonText.toLowerCase().contains("random")) {
            this.iconType = IconType.RANDOM;
        } else if (buttonText.toLowerCase().contains("delete")) {
            this.iconType = IconType.DELETE;
        }
    }

    public ModernAltButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, Style style) {
        this(buttonId, x, y, widthIn, heightIn, (String) null, buttonText, style);
    }

    public ModernAltButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        this(buttonId, x, y, widthIn, heightIn, (String) null, buttonText, Style.NORMAL);
    }

    public void setIconType(IconType iconType) {
        this.iconType = iconType;
    }

    public IconType getIconType() {
        return this.iconType;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return this.active;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        if (this.hovered) {
            hoverProgress = Math.min(1.0F, hoverProgress + 0.14F);
        } else {
            hoverProgress = Math.max(0.0F, hoverProgress - 0.14F);
        }

        float radius = 6.0F;
        float x = this.xPosition;
        float y = this.yPosition;
        float w = this.width;
        float h = this.height;

        int bgColor;
        int borderColor;
        int textColor;
        int iconColor;

        // Palette Constants
        int cPrimaryRed = 0xFFE52B3F;
        int cDarkRed    = 0xFF65151F;
        int cHoverRed   = 0xFFFF3D52;
        int cBorder     = 0xFF29252C;
        int cBorderHov  = 0xFF453F4A;
        int cNormalText = 0xFFD8D4DA;
        int cMutedText  = 0xFFA8A2AA;
        int cBrightText = 0xFFF2EFF3;

        if (style == Style.TAB) {
            if (active) {
                bgColor = 0xFF221218;
                borderColor = cPrimaryRed;
                textColor = cPrimaryRed;
                iconColor = cPrimaryRed;

                // Subtle red glow for active tab
                RenderUtil.drawRoundedOutline(x - 1.0F, y - 1.0F, w + 2.0F, h + 2.0F, radius + 1.0F, 1.0F, 0x44E52B3F);
            } else {
                int bgR = (int) (0x12 + hoverProgress * 8);
                int bgG = (int) (0x10 + hoverProgress * 6);
                int bgB = (int) (0x15 + hoverProgress * 9);
                bgColor = (0xFF << 24) | (bgR << 16) | (bgG << 8) | bgB;

                borderColor = (hoverProgress > 0.1F) ? cBorderHov : cBorder;
                textColor = (hoverProgress > 0.1F) ? cBrightText : cNormalText;
                iconColor = (hoverProgress > 0.1F) ? cPrimaryRed : cMutedText;
            }
        } else if (style == Style.DESTRUCTIVE) {
            int bgR = (int) (0x16 + hoverProgress * 12);
            int bgG = (int) (0x0C + hoverProgress * 4);
            int bgB = (int) (0x10 + hoverProgress * 6);
            bgColor = (0xFF << 24) | (bgR << 16) | (bgG << 8) | bgB;

            borderColor = (hoverProgress > 0.1F) ? cPrimaryRed : (0x99000000 | (cDarkRed & 0xFFFFFF));
            textColor = (hoverProgress > 0.1F) ? cHoverRed : cPrimaryRed;
            iconColor = (hoverProgress > 0.1F) ? cHoverRed : cPrimaryRed;

            if (hoverProgress > 0.05F) {
                int glowAlpha = (int) (hoverProgress * 60);
                RenderUtil.drawRoundedOutline(x - 1.0F, y - 1.0F, w + 2.0F, h + 2.0F, radius + 1.0F, 1.0F, (glowAlpha << 24) | (cPrimaryRed & 0xFFFFFF));
            }
        } else if (style == Style.PRIMARY) {
            int bgR = (int) (0x1E + hoverProgress * 14);
            int bgG = (int) (0x10 + hoverProgress * 6);
            int bgB = (int) (0x15 + hoverProgress * 8);
            bgColor = (0xFF << 24) | (bgR << 16) | (bgG << 8) | bgB;

            borderColor = (hoverProgress > 0.1F) ? cPrimaryRed : 0x88E52B3F;
            textColor = (hoverProgress > 0.1F) ? cBrightText : cNormalText;
            iconColor = (hoverProgress > 0.1F) ? cHoverRed : cPrimaryRed;
        } else {
            // NORMAL
            int bgR = (int) (0x12 + hoverProgress * 10);
            int bgG = (int) (0x10 + hoverProgress * 8);
            int bgB = (int) (0x15 + hoverProgress * 11);
            bgColor = (0xFF << 24) | (bgR << 16) | (bgG << 8) | bgB;

            borderColor = (hoverProgress > 0.1F) ? cBorderHov : cBorder;
            textColor = (hoverProgress > 0.1F) ? cBrightText : cNormalText;
            iconColor = (hoverProgress > 0.1F) ? cPrimaryRed : cMutedText;
        }

        // Draw modern background
        RenderUtil.drawRoundedRect(x, y, w, h, radius, bgColor);

        // Draw 1px crisp subtle border
        RenderUtil.drawRoundedOutline(x, y, w, h, radius, 1.0F, borderColor);

        // Calculate Text & Icon dimensions
        int textW = (FontManager.sfSemiBold14 != null) ? FontManager.sfSemiBold14.getStringWidth(this.displayString) : mc.fontRendererObj.getStringWidth(this.displayString);

        if (this.iconType != IconType.NONE) {
            float iconSize = 12.0F;
            int gap = 7;
            float totalW = iconSize + gap + textW;
            float startContentX = x + (w - totalW) / 2.0F;
            float iconCX = startContentX + iconSize / 2.0F;
            float iconCY = y + h / 2.0F;

            // Render Vector Icon
            if (this.iconType == IconType.USER) {
                RenderUtil.drawUserSolidIcon(iconCX, iconCY, 12.0F, iconColor);
            } else if (this.iconType == IconType.CLIPBOARD_USER) {
                RenderUtil.drawClipboardUserIcon(iconCX, iconCY, 12.0F, iconColor);
            } else if (this.iconType == IconType.REFRESH) {
                RenderUtil.drawRefreshIcon(iconCX, iconCY, 12.0F, iconColor);
            } else if (this.iconType == IconType.MICROSOFT) {
                RenderUtil.drawMicrosoftIcon(iconCX, iconCY, 12.0F, iconColor);
            } else if (this.iconType == IconType.COOKIE_BITE || this.iconType == IconType.COOKIE) {
                RenderUtil.drawCookieBiteIcon(iconCX, iconCY, 12.0F, iconColor);
            } else if (this.iconType == IconType.CLIPBOARD) {
                RenderUtil.drawClipboardIcon(iconCX, iconCY, 12.0F, iconColor);
            } else if (this.iconType == IconType.RANDOM) {
                RenderUtil.drawRandomDiceIcon(iconCX, iconCY, 12.0F, iconColor);
            } else if (this.iconType == IconType.DIRECT) {
                RenderUtil.drawDirectKeyIcon(iconCX, iconCY, 12.0F, iconColor);
            } else if (this.iconType == IconType.DELETE) {
                RenderUtil.drawTrashIcon(iconCX, iconCY, 12.0F, iconColor);
            } else if (this.iconType == IconType.PLUS) {
                RenderUtil.drawPlusIcon(iconCX, iconCY, 10.0F, 1.8F, iconColor);
            }

            // Draw Text
            float textStartX = startContentX + iconSize + gap;
            if (FontManager.sfSemiBold14 != null) {
                FontManager.sfSemiBold14.drawString(this.displayString, textStartX, y + (h - FontManager.sfSemiBold14.getHeight()) / 2.0F + 1.0F, textColor);
            } else {
                mc.fontRendererObj.drawString(this.displayString, (int) textStartX, (int) (y + (h - 8) / 2), textColor);
            }
        } else if (this.iconGlyph != null && !this.iconGlyph.isEmpty() && FontManager.faRegular16 != null) {
            int iconW = FontManager.faRegular16.getStringWidth(this.iconGlyph);
            int gap = 6;
            int totalW = iconW + gap + textW;
            float startContentX = x + (w - totalW) / 2.0F;

            FontManager.faRegular16.drawString(this.iconGlyph, startContentX, y + (h - FontManager.faRegular16.getHeight()) / 2.0F + 1.0F, iconColor);

            float textStartX = startContentX + iconW + gap;
            if (FontManager.sfSemiBold14 != null) {
                FontManager.sfSemiBold14.drawString(this.displayString, textStartX, y + (h - FontManager.sfSemiBold14.getHeight()) / 2.0F + 1.0F, textColor);
            } else {
                mc.fontRendererObj.drawString(this.displayString, (int) textStartX, (int) (y + (h - 8) / 2), textColor);
            }
        } else {
            // Centered Text Only
            if (FontManager.sfSemiBold14 != null) {
                FontManager.sfSemiBold14.drawCenteredString(this.displayString, x + w / 2.0F, y + (h - FontManager.sfSemiBold14.getHeight()) / 2.0F + 1.0F, textColor);
            } else if (FontManager.sfMedium14 != null) {
                FontManager.sfMedium14.drawCenteredString(this.displayString, x + w / 2.0F, y + (h - FontManager.sfMedium14.getHeight()) / 2.0F + 1.0F, textColor);
            } else {
                mc.fontRendererObj.drawString(this.displayString, (int) (x + (w - mc.fontRendererObj.getStringWidth(this.displayString)) / 2), (int) (y + (h - 8) / 2), textColor);
            }
        }
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }
}

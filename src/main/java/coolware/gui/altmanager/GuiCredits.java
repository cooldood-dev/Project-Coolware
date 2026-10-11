package coolware.gui.altmanager;

import coolware.Coolware;
import coolware.font.FontManager;
import coolware.gui.menu.ParticleSystem;
import coolware.util.RenderUtil;
import coolware.util.RoundedUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.io.IOException;

public class GuiCredits extends GuiScreen {

    private final GuiScreen parentScreen;
    private final ParticleSystem particleSystem = new ParticleSystem();

    private static final int C_BG_START   = 0xFF0D0B0E;
    private static final int C_BG_END     = 0xFF140F18;
    private static final int C_CARD_BG    = 0xFF151219;
    private static final int C_CARD_HOV   = 0xFF1B1720;
    private static final int C_BORDER     = 0xFF28232C;
    private static final int C_BORDER_HOV = 0xFF423B48;
    private static final int C_RED_PRIM   = 0xFFE52B3F;
    private static final int C_MUTED_TEXT = 0xFFA8A2AA;
    private static final int C_BRIGHT_TXT = 0xFFF2EFF3;

    private static final CreditEntry[] CREDITS = new CreditEntry[] {
        new CreditEntry("Arraylist", "Rise", "Clean, animated, and responsive modules list display"),
        new CreditEntry("Killaura & Velocity", "Nespola", "Target combat logic and precise knockback handling"),
        new CreditEntry("Scaffold", "Cat", "Rotations, placement heuristics, and tower bridging"),
        new CreditEntry("All visuals", "cooldood", "Complete visual design, themes, and aesthetic overhaul"),
        new CreditEntry("Clickgui", "pisskot", "Interactive dropdown panels, components, and layout"),
        new CreditEntry("Notis & Session Info (Modern)", "Ambient", "Modern toast alerts and sleek session statistics hud")
    };

    public GuiCredits(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(false);
        this.buttonList.clear();

        int btnW = 140;
        int btnH = 32;
        int btnX = (this.width - btnW) / 2;
        int btnY = this.height - 46;

        this.buttonList.add(new ModernAltButton(0, btnX, btnY, btnW, btnH, ModernAltButton.IconType.NONE, "Back", ModernAltButton.Style.NORMAL));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. Background Gradient & Tint
        RenderUtil.drawHorizontalGradient(0, 0, this.width, this.height, C_BG_START, C_BG_END);
        RenderUtil.drawGradientRect(0, 0, this.width, this.height, 0x33000000, 0x88000000);

        // 2. Ambient Particles
        this.particleSystem.updateAndRender(this.width, this.height, mouseX, mouseY);

        // 3. Header Section
        float headerCenterY = 24.0F;
        String title = "Credits & Acknowledgments";
        if (FontManager.sfBold28 != null) {
            float tw = FontManager.sfBold28.getStringWidth(title);
            FontManager.sfBold28.drawStringWithShadow(title, (this.width - tw) / 2.0F, headerCenterY, C_BRIGHT_TXT);
        } else if (FontManager.sfBold22 != null) {
            float tw = FontManager.sfBold22.getStringWidth(title);
            FontManager.sfBold22.drawStringWithShadow(title, (this.width - tw) / 2.0F, headerCenterY, C_BRIGHT_TXT);
        } else {
            this.fontRendererObj.drawStringWithShadow("\u00a7l" + title, (this.width - this.fontRendererObj.getStringWidth(title)) / 2, (int) headerCenterY, C_BRIGHT_TXT);
        }

        String sub = "Special thanks to all the contributors and projects behind Coolware v" + Coolware.VERSION;
        float subY = headerCenterY + 24.0F;
        if (FontManager.sfSemiBold14 != null) {
            float sw = FontManager.sfSemiBold14.getStringWidth(sub);
            FontManager.sfSemiBold14.drawStringWithShadow(sub, (this.width - sw) / 2.0F, subY, C_MUTED_TEXT);
        } else {
            this.fontRendererObj.drawStringWithShadow(sub, (this.width - this.fontRendererObj.getStringWidth(sub)) / 2, (int) subY, C_MUTED_TEXT);
        }

        // 4. Credits Grid Cards
        int containerW = Math.min(680, this.width - 40);
        int containerX = (this.width - containerW) / 2;
        int gridStartY = 76;
        int columns = (this.width >= 560) ? 2 : 1;
        int cardGap = 12;
        int cardW = (columns == 2) ? ((containerW - cardGap) / 2) : containerW;
        int cardH = 48;

        for (int i = 0; i < CREDITS.length; i++) {
            CreditEntry entry = CREDITS[i];
            int col = (columns == 2) ? (i % 2) : 0;
            int row = (columns == 2) ? (i / 2) : i;

            int x = containerX + col * (cardW + cardGap);
            int y = gridStartY + row * (cardH + cardGap);

            boolean isHovered = mouseX >= x && mouseX <= x + cardW && mouseY >= y && mouseY <= y + cardH;
            int cardBg = isHovered ? C_CARD_HOV : C_CARD_BG;
            int cardBorder = isHovered ? C_BORDER_HOV : C_BORDER;

            // Draw Card Container
            RenderUtil.drawRoundedRect(x, y, cardW, cardH, 7.0F, cardBg);
            RenderUtil.drawRoundedOutline(x, y, cardW, cardH, 7.0F, 1.0F, cardBorder);

            // Left decorative accent pill
            int accentColor = isHovered ? C_RED_PRIM : 0xFF7D1B26;
            RenderUtil.drawRoundedRect(x + 3.0F, y + 8.0F, 3.0F, cardH - 16.0F, 1.5F, accentColor);

            // Title & Author Text
            float textX = x + 16.0F;
            float textY = y + 9.0F;

            if (FontManager.sfBold16 != null) {
                FontManager.sfBold16.drawString(entry.feature, textX, textY, C_BRIGHT_TXT);
                float fWidth = FontManager.sfBold16.getStringWidth(entry.feature);

                // Badge / Tag with Author
                float tagX = textX + fWidth + 8.0F;
                float tagW = (FontManager.sfSemiBold14 != null ? FontManager.sfSemiBold14.getStringWidth(entry.author) : 30) + 12.0F;
                float tagH = 15.0F;
                float tagY = textY - 1.0F;

                RenderUtil.drawRoundedRect(tagX, tagY, tagW, tagH, 4.0F, 0x33E52B3F);
                RenderUtil.drawRoundedOutline(tagX, tagY, tagW, tagH, 4.0F, 0.8F, 0x66E52B3F);

                if (FontManager.sfSemiBold14 != null) {
                    FontManager.sfSemiBold14.drawString(entry.author, tagX + 6.0F, tagY + 3.0F, C_RED_PRIM);
                }
            } else {
                this.fontRendererObj.drawStringWithShadow(entry.feature + " \u00a7c[" + entry.author + "]", (int) textX, (int) textY, C_BRIGHT_TXT);
            }

            // Description subtext
            float descY = textY + 16.0F;
            if (FontManager.sfRegular12 != null) {
                FontManager.sfRegular12.drawString(entry.description, textX, descY, C_MUTED_TEXT);
            } else if (FontManager.sfRegular14 != null) {
                FontManager.sfRegular14.drawString(entry.description, textX, descY, C_MUTED_TEXT);
            } else {
                this.fontRendererObj.drawString(entry.description, (int) textX, (int) descY, C_MUTED_TEXT);
            }
        }

        // 5. Draw Buttons
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static class CreditEntry {
        final String feature;
        final String author;
        final String description;

        CreditEntry(String feature, String author, String description) {
            this.feature = feature;
            this.author = author;
            this.description = description;
        }
    }
}

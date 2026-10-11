package coolware.gui.clickgui;

import coolware.config.CloudConfigManager;
import coolware.font.CFontRenderer;
import coolware.font.FontManager;
import coolware.module.visuals.Theme;
import coolware.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Interactive Cloud Configs Tab Panel in ClickGUI.
 * Displays available cloud configs from S3 bucket.
 * Users can click to instantly download and load them.
 */
public class ConfigsPanel {
    private int x;
    private int y;
    private int width;
    private int headerHeight;
    private boolean isExpanded;
    private boolean isDragging;
    private int dragX;
    private int dragY;

    // Scrolling
    private float scrollOffset = 0.0F;
    private float scrollTarget = 0.0F;
    private float maxScroll = 0.0F;
    private static final float SCROLL_SPEED = 22.0F;

    public static int getHeaderHeight() {
        return Math.max(22, ClickGuiFonts.bold().getHeight() + 8);
    }

    public static int getItemRowHeight() {
        return Math.max(18, ClickGuiFonts.regular().getHeight() + 6);
    }

    public ConfigsPanel(int x, int y) {
        this.x = x;
        this.y = y;
        this.width = 142;
        this.headerHeight = getHeaderHeight();
        this.isExpanded = true;
        this.isDragging = false;
        CloudConfigManager.refreshAsync();
    }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setWidth(int width) { this.width = width; }

    private int computeContentHeight() {
        this.headerHeight = getHeaderHeight();
        int itemH = getItemRowHeight();
        int contentHeight = headerHeight + itemH + 6; // Status / refresh button row
        if (isExpanded) {
            List<String> configs = CloudConfigManager.getCachedConfigs();
            contentHeight += Math.max(1, configs.size()) * (itemH + 2);
            contentHeight += 6;
        }
        return contentHeight;
    }

    private int computeVisibleHeight() {
        int screenH = Minecraft.getMinecraft().currentScreen != null ? Minecraft.getMinecraft().currentScreen.height : 240;
        return Math.max(headerHeight + 20, screenH - y - 8);
    }

    private void updateScroll() {
        if (!isExpanded) {
            maxScroll = 0.0F;
            scrollTarget = 0.0F;
        } else {
            int contentHeight = computeContentHeight();
            int visibleHeight = computeVisibleHeight();
            maxScroll = Math.max(0.0F, contentHeight - visibleHeight);
            if (scrollTarget > maxScroll) scrollTarget = maxScroll;
            if (scrollTarget < 0.0F) scrollTarget = 0.0F;
        }
        scrollOffset += (scrollTarget - scrollOffset) * 0.35F;
        if (Math.abs(scrollTarget - scrollOffset) < 0.25F) {
            scrollOffset = scrollTarget;
        }
    }

    public void draw(int mouseX, int mouseY) {
        if (isDragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }

        updateScroll();

        Theme theme = Theme.getInstance();
        int accentColor = theme != null ? theme.getColor(0) : 0xFFE00000;
        CFontRenderer font = ClickGuiFonts.bold();
        CFontRenderer textFont = ClickGuiFonts.regular();

        int contentHeight = computeContentHeight();
        int visibleHeight = computeVisibleHeight();
        int panelHeight = isExpanded ? Math.min(contentHeight, visibleHeight) : headerHeight;

        // 1. Draw outer shadow & dark panel body
        RenderUtil.drawRoundedRect(x + 2, y + 3, width, panelHeight, 6.0F, 0x55000000);
        RenderUtil.drawRoundedRect(x, y, width, panelHeight, 6.0F, 0xDD0A0B0E);

        // 2. Red outline
        int borderRed = (accentColor & 0x00FFFFFF) | 0xEE000000;
        RenderUtil.drawRoundedOutline(x, y, width, panelHeight, 6.0F, 1.5F, borderRed);

        // 3. Header title
        String title = "CONFIGS";
        if (font != null) {
            font.drawStringWithShadow(title, x + 8, y + (headerHeight - font.getHeight()) / 2.0F + 1, 0xFFF2F2F2);
            String icon = isExpanded ? "-" : "+";
            font.drawStringWithShadow(icon, x + width - font.getStringWidth(icon) - 8, y + (headerHeight - font.getHeight()) / 2.0F + 1, 0xFFDDDDDD);
        } else {
            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            fr.drawStringWithShadow(title, x + 8, y + (headerHeight - 8) / 2 + 1, 0xFFF2F2F2);
            String icon = isExpanded ? "-" : "+";
            fr.drawStringWithShadow(icon, x + width - fr.getStringWidth(icon) - 8, y + (headerHeight - 8) / 2 + 1, 0xFFDDDDDD);
        }

        RenderUtil.drawRect(x + 6, y + headerHeight - 1, x + width - 6, y + headerHeight, 0x1AFFFFFF);

        // 4. Content Area
        if (isExpanded) {
            GlStateManager.pushMatrix();
            float scaleFactor = Minecraft.getMinecraft().gameSettings.guiScale;
            int clipBottom = y + panelHeight - 1;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(
                    (int) ((x + 2) * scaleFactor),
                    (int) (Minecraft.getMinecraft().displayHeight - clipBottom * scaleFactor),
                    (int) ((width - 4) * scaleFactor),
                    (int) ((panelHeight - headerHeight - 1) * scaleFactor));

            int currentY = (int) (y + headerHeight + 3 - scrollOffset);

            int itemH = getItemRowHeight();
            // Refresh / Status row
            boolean refreshHovered = mouseX >= x + 6 && mouseX <= x + width - 6 && mouseY >= currentY && mouseY <= currentY + itemH;
            RenderUtil.drawRoundedRect(x + 6, currentY, width - 12, itemH, 4.0F, refreshHovered ? 0x44FFFFFF : 0x22FFFFFF);
            String statusText = CloudConfigManager.isFetching() ? "Refreshing..." : "Refresh Cloud";
            if (textFont != null) {
                textFont.drawCenteredString(statusText, x + width / 2.0F, currentY + (itemH - textFont.getHeight()) / 2.0F + 1, refreshHovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            }
            currentY += itemH + 4;

            List<String> configs = CloudConfigManager.getCachedConfigs();
            if (configs.isEmpty()) {
                if (textFont != null) {
                    textFont.drawCenteredString(CloudConfigManager.getLastStatus(), x + width / 2.0F, currentY + (itemH - textFont.getHeight()) / 2.0F + 1, 0xFF888888);
                }
            } else {
                for (String cfg : configs) {
                    boolean itemHovered = mouseX >= x + 6 && mouseX <= x + width - 6 && mouseY >= currentY && mouseY <= currentY + itemH;
                    RenderUtil.drawRoundedRect(x + 6, currentY, width - 12, itemH, 4.0F, itemHovered ? 0x33FF2233 : 0x18FFFFFF);

                    String cleanName = cfg.replace(".cfg", "");
                    if (textFont != null) {
                        String loadText = "LOAD";
                        int loadWidth = textFont.getStringWidth(loadText);
                        int maxNameWidth = width - 24 - loadWidth;
                        String renderedName = cleanName;
                        if (textFont.getStringWidth(renderedName) > maxNameWidth) {
                            while (renderedName.length() > 3 && textFont.getStringWidth(renderedName + "..") > maxNameWidth) {
                                renderedName = renderedName.substring(0, renderedName.length() - 1);
                            }
                            renderedName = renderedName + "..";
                        }
                        float textY = currentY + (itemH - textFont.getHeight()) / 2.0F + 1;
                        textFont.drawString(renderedName, x + 10, textY, itemHovered ? 0xFFFFFFFF : 0xFFCCCCCC);
                        textFont.drawString(loadText, x + width - loadWidth - 10, textY, accentColor);
                    }
                    currentY += itemH + 2;
                }
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.popMatrix();
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHoveredHeader(mouseX, mouseY)) {
            if (mouseButton == 0) {
                isDragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
            } else if (mouseButton == 1) {
                isExpanded = !isExpanded;
            }
            return;
        }

        if (!isExpanded) return;

        int itemH = getItemRowHeight();
        int currentY = (int) (y + headerHeight + 3 - scrollOffset);

        // Click refresh
        if (mouseX >= x + 6 && mouseX <= x + width - 6 && mouseY >= currentY && mouseY <= currentY + itemH) {
            CloudConfigManager.refreshAsync();
            return;
        }
        currentY += itemH + 4;

        List<String> configs = CloudConfigManager.getCachedConfigs();
        for (String cfg : configs) {
            if (mouseX >= x + 6 && mouseX <= x + width - 6 && mouseY >= currentY && mouseY <= currentY + itemH) {
                if (mouseButton == 0) {
                    // Instantly download and load configuration
                    CloudConfigManager.downloadAndLoad(cfg);
                }
                return;
            }
            currentY += itemH + 2;
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            isDragging = false;
        }
    }

    public boolean mouseScrolled(int mouseX, int mouseY, int wheelDelta) {
        if (!isExpanded) return false;
        if (isHovered(mouseX, mouseY) && wheelDelta != 0) {
            scrollTarget -= (wheelDelta / 120.0F) * SCROLL_SPEED;
            if (scrollTarget < 0.0F) scrollTarget = 0.0F;
            if (scrollTarget > maxScroll) scrollTarget = maxScroll;
            return true;
        }
        return false;
    }

    public void handleMouseScroll(int mouseX, int mouseY, int dWheel) {
        mouseScrolled(mouseX, mouseY, dWheel);
    }

    private boolean isHoveredHeader(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        int panelHeight = isExpanded ? computeContentHeight() : headerHeight;
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + panelHeight;
    }
}

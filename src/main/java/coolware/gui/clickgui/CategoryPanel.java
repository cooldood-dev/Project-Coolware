package coolware.gui.clickgui;

import coolware.Coolware;
import coolware.font.CFontRenderer;
import coolware.font.FontManager;
import coolware.module.Category;
import coolware.module.Module;
import coolware.module.visuals.Theme;
import coolware.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class CategoryPanel {
    private final Category category;
    private int x;
    private int y;
    private int width;
    private int headerHeight;
    private boolean isExpanded;
    private boolean isDragging;
    private int dragX;
    private int dragY;
    private final List<ModuleButton> buttons = new ArrayList<ModuleButton>();

    // Scrolling
    private float scrollOffset = 0.0F;
    private float scrollTarget = 0.0F;
    private float maxScroll = 0.0F;
    private static final float SCROLL_SPEED = 22.0F;

    public static int getHeaderHeight() {
        return Math.max(22, ClickGuiFonts.bold().getHeight() + 8);
    }

    public CategoryPanel(Category category, int x, int y) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.width = 142;
        this.headerHeight = getHeaderHeight();
        this.isExpanded = true;
        this.isDragging = false;

        refreshModules();
    }

    public void refreshModules() {
        buttons.clear();
        if (Coolware.getInstance() != null && Coolware.getInstance().getModuleManager() != null) {
            List<Module> categoryModules = Coolware.getInstance().getModuleManager().getModulesByCategory(category);
            for (Module mod : categoryModules) {
                buttons.add(new ModuleButton(mod, this));
            }
        }
        scrollOffset = 0.0F;
        scrollTarget = 0.0F;
        maxScroll = 0.0F;
    }

    private int computeContentHeight() {
        this.headerHeight = getHeaderHeight();
        int contentHeight = headerHeight;
        if (isExpanded) {
            for (ModuleButton button : buttons) {
                contentHeight += button.getHeight();
            }
            contentHeight += 4; // Bottom padding
        }
        return contentHeight;
    }

    private int computeVisibleHeight() {
        Minecraft mc = Minecraft.getMinecraft();
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
            if (scrollTarget > maxScroll) {
                scrollTarget = maxScroll;
            }
            if (scrollTarget < 0.0F) {
                scrollTarget = 0.0F;
            }
        }
        // Smooth animate towards target
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

        int contentHeight = computeContentHeight();
        int visibleHeight = computeVisibleHeight();
        int panelHeight = isExpanded ? Math.min(contentHeight, visibleHeight) : contentHeight;

        // 1. Draw outer shadow for entire floating card
        RenderUtil.drawRoundedRect(x + 2, y + 3, width, panelHeight, 6.0F, 0x55000000);

        // 2. Draw deep dark-shaded translucent rounded background card for each tab individually
        RenderUtil.drawRoundedRect(x, y, width, panelHeight, 6.0F, 0xDD0A0B0E);

        // 3. Draw clear rounded outline with red around the perimeter of each tab individually
        int borderRed = (accentColor & 0x00FFFFFF) | 0xEE000000; // Crisp vibrant red outline (93% alpha)
        RenderUtil.drawRoundedOutline(x, y, width, panelHeight, 6.0F, 1.5F, borderRed);

        // 4. Header title & icon (clean typography, no accent bars)
        String title = category.getName().toUpperCase();
        if (font != null) {
            font.drawStringWithShadow(title, x + 8, y + (headerHeight - font.getHeight()) / 2.0F + 1, 0xFFF2F2F2);
            String icon = isExpanded ? "-" : "+";
            int iconWidth = font.getStringWidth(icon);
            font.drawStringWithShadow(icon, x + width - iconWidth - 8, y + (headerHeight - font.getHeight()) / 2.0F + 1, 0xFFDDDDDD);
        } else {
            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            fr.drawStringWithShadow(title, x + 8, y + (headerHeight - 8) / 2 + 1, 0xFFF2F2F2);
            String icon = isExpanded ? "-" : "+";
            int iconWidth = fr.getStringWidth(icon);
            fr.drawStringWithShadow(icon, x + width - iconWidth - 8, y + (headerHeight - 8) / 2 + 1, 0xFFDDDDDD);
        }

        // Clean subtle separator line under header without accent bars
        RenderUtil.drawRect(x + 6, y + headerHeight - 1, x + width - 6, y + headerHeight, 0x1AFFFFFF);

        // 5. Draw Module items, clipped to the panel card and shifted by the scroll offset
        if (isExpanded) {
            GlStateManager.pushMatrix();
            // Enable scissor box (GL coords: Y is flipped, and we account for the GUI scale factor)
            float scaleFactor = Minecraft.getMinecraft().gameSettings.guiScale;
            int clipBottom = y + panelHeight - 1;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(
                    (int) ((x + 2) * scaleFactor),
                    (int) (Minecraft.getMinecraft().displayHeight - clipBottom * scaleFactor),
                    (int) ((width - 4) * scaleFactor),
                    (int) ((panelHeight - headerHeight - 1) * scaleFactor));

            int currentY = (int) (y + headerHeight + 2 - scrollOffset);
            for (ModuleButton button : buttons) {
                // Skip buttons entirely outside the visible area
                if (currentY + button.getHeight() >= y + headerHeight && currentY <= clipBottom) {
                    button.draw(x, currentY, width, mouseX, mouseY);
                }
                currentY += button.getHeight();
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.popMatrix();
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHoveredHeader(mouseX, mouseY)) {
            if (mouseButton == 0) {
                // Left click on header -> drag panel
                isDragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
            } else if (mouseButton == 1) {
                // Right click on header -> toggle collapse/expand
                isExpanded = !isExpanded;
            }
            return;
        }

        if (isExpanded && isWithinPanelBody(mouseX, mouseY)) {
            int currentY = (int) (y + headerHeight + 2 - scrollOffset);
            for (ModuleButton button : buttons) {
                if (currentY + button.getHeight() > y + headerHeight && currentY <= y + computeVisibleHeight()) {
                    button.mouseClicked(x, currentY, width, mouseX, mouseY, mouseButton);
                }
                currentY += button.getHeight();
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            isDragging = false;
        }
        if (isExpanded) {
            for (ModuleButton button : buttons) {
                button.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    public boolean mouseScrolled(int mouseX, int mouseY, int wheelDelta) {
        if (!isExpanded) {
            return false;
        }

        // Panel-level scroll when the cursor is anywhere over this panel card
        if (maxScroll > 0.0F && isWithinPanelBody(mouseX, mouseY)) {
            scrollTarget -= (wheelDelta / 120.0F) * SCROLL_SPEED;
            scrollTarget = Math.max(0.0F, Math.min(maxScroll, scrollTarget));
            return true;
        }

        // Fallback: per-setting slider scroll (only when the panel is not scrollable)
        int currentY = (int) (y + headerHeight + 2 - scrollOffset);
        for (ModuleButton button : buttons) {
            if (button.mouseScrolled(x, currentY, width, mouseX, mouseY, wheelDelta)) {
                return true;
            }
            currentY += button.getHeight();
        }
        return false;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (isExpanded) {
            for (ModuleButton button : buttons) {
                button.keyTyped(typedChar, keyCode);
            }
        }
    }

    public boolean hasActiveInput() {
        if (isExpanded) {
            for (ModuleButton button : buttons) {
                if (button.hasActiveInput()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWithinPanelBody(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y + headerHeight && mouseY <= y + Math.min(computeContentHeight(), computeVisibleHeight());
    }

    private boolean isHoveredHeader(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight;
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}

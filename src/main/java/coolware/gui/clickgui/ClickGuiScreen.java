package coolware.gui.clickgui;

import coolware.Coolware;
import coolware.gui.hud.HUDOverlay;
import coolware.module.Category;
import coolware.module.visuals.HUD;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends GuiScreen {
    private static final long ANIMATION_DURATION_MS = 200L;
    private static final int PANEL_WIDTH = 142;
    private static final int PANEL_SPACING = 14;

    // The original 5 tabs/categories: Combat, Movement, Player, Visuals, Client
    private final Category[] originalCategories = new Category[] {
            Category.COMBAT,
            Category.MOVEMENT,
            Category.PLAYER,
            Category.VISUALS,
            Category.CLIENT
    };

    private final List<CategoryPanel> panels = new ArrayList<CategoryPanel>();
    private ConfigsPanel configsPanel;
    private long animationStartedAt;
    private boolean closing;

    public ClickGuiScreen() {
        createPanels();
    }

    private void createPanels() {
        panels.clear();
        int totalTabs = originalCategories.length + 1;
        int totalWidth = totalTabs * PANEL_WIDTH + (totalTabs - 1) * PANEL_SPACING;

        // Position initial layout centered if resolution is known, otherwise default offsets
        int startX = 25;
        int startY = 40;

        for (int i = 0; i < originalCategories.length; i++) {
            Category cat = originalCategories[i];
            panels.add(new CategoryPanel(cat, startX + i * (PANEL_WIDTH + PANEL_SPACING), startY));
        }
        configsPanel = new ConfigsPanel(startX + originalCategories.length * (PANEL_WIDTH + PANEL_SPACING), startY);
    }

    @Override
    public void initGui() {
        animationStartedAt = System.currentTimeMillis();
        closing = false;

        // Refresh panels to ensure sync with all client modules
        int totalTabs = originalCategories.length + 1;
        int totalWidth = totalTabs * PANEL_WIDTH + (totalTabs - 1) * PANEL_SPACING;
        int startX = Math.max(15, (this.width - totalWidth) / 2);
        int startY = Math.max(25, (this.height - 380) / 2);

        if (panels.isEmpty() || panels.size() != originalCategories.length) {
            panels.clear();
            for (int i = 0; i < originalCategories.length; i++) {
                panels.add(new CategoryPanel(originalCategories[i], startX + i * (PANEL_WIDTH + PANEL_SPACING), startY));
            }
        } else {
            for (int i = 0; i < panels.size(); i++) {
                CategoryPanel panel = panels.get(i);
                panel.setWidth(PANEL_WIDTH);
                panel.setX(startX + i * (PANEL_WIDTH + PANEL_SPACING));
                panel.setY(startY);
                panel.refreshModules();
            }
        }

        if (configsPanel == null) {
            configsPanel = new ConfigsPanel(startX + originalCategories.length * (PANEL_WIDTH + PANEL_SPACING), startY);
        } else {
            configsPanel.setWidth(PANEL_WIDTH);
            configsPanel.setX(startX + originalCategories.length * (PANEL_WIDTH + PANEL_SPACING));
            configsPanel.setY(startY);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (closing && System.currentTimeMillis() - animationStartedAt >= ANIMATION_DURATION_MS) {
            mc.displayGuiScreen(null);
            if (mc.currentScreen == null) {
                mc.setIngameFocus();
            }
            return;
        }

        // Ensure clean 2D overlay state
        mc.entityRenderer.setupOverlayRendering();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Keep the game visible behind the ClickGUI instead of dimming it almost completely.
        drawRect(0, 0, this.width, this.height, 0x40000000);

        // Draw HUD overlay preview
        HUD hud = Coolware.getInstance().getModuleManager().getModule(HUD.class);
        if (hud != null && hud.isToggled()) {
            HUDOverlay.render();
        }
        // Draw the 5 Category panels + Configs panel
        for (CategoryPanel panel : panels) {
            panel.draw(mouseX, mouseY);
        }
        if (configsPanel != null) {
            configsPanel.draw(mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (configsPanel != null) {
            configsPanel.mouseClicked(mouseX, mouseY, mouseButton);
        }
        // Process panels backwards so front-most gets priority
        for (int i = panels.size() - 1; i >= 0; i--) {
            panels.get(i).mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (configsPanel != null) {
            configsPanel.mouseReleased(mouseX, mouseY, state);
        }
        for (CategoryPanel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, state);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheelDelta = Mouse.getEventDWheel();
        if (wheelDelta == 0) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = Mouse.getEventX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getEventY() * sr.getScaledHeight() / mc.displayHeight - 1;

        if (configsPanel != null) {
            configsPanel.mouseScrolled(mouseX, mouseY, wheelDelta);
        }
        for (CategoryPanel panel : panels) {
            if (panel.mouseScrolled(mouseX, mouseY, wheelDelta)) {
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        boolean hadInput = false;
        for (CategoryPanel panel : panels) {
            if (panel.hasActiveInput()) {
                hadInput = true;
            }
            panel.keyTyped(typedChar, keyCode);
        }

        if (hadInput) {
            return;
        }

        // Close ClickGUI on ESC or RSHIFT
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RSHIFT || keyCode == 54 || keyCode == 1) {
            if (!closing) {
                closing = true;
                animationStartedAt = System.currentTimeMillis();
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

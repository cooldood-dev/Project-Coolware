package coolware.gui.clickgui.novoline;

import coolware.module.Category;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NovolineDropdownGUI extends GuiScreen {

    private final List<NovolineTab> tabs = new CopyOnWriteArrayList<NovolineTab>();
    private int dragX;
    private int dragY;

    public NovolineDropdownGUI() {}

    @Override
    public void initGui() {
        float x = 40;

        if (tabs.isEmpty()) {
            for (Category category : Category.values()) {
                tabs.add(new NovolineTab(category, x, 10));
                x += 110;
            }
            tabs.add(new NovolineConfigTab(x, 10));
        }

        for (NovolineTab tab : tabs) {
            for (NovolineModuleButton module : tab.modules) {
                module.fraction = 0;
                for (NovolineSettingElement setting : module.settings) {
                    setting.setPercent(0);
                }
            }
            if (tab instanceof NovolineConfigTab) {
                ((NovolineConfigTab) tab).refreshConfigs();
            }
        }

        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GL11.glPushMatrix();
        ScaledResolution scaledResolution = new ScaledResolution(mc);

        int mouseCoordsX = mouseX;
        int mouseCoordsY = mouseY;
        switch (mc.gameSettings.guiScale) {
            case 0:
                mouseCoordsX *= 2;
                mouseCoordsY *= 2;
                break;
            case 1:
                mouseCoordsX *= 0.5;
                mouseCoordsY *= 0.5;
                break;
            case 3:
                mouseCoordsX *= 1.5;
                mouseCoordsY *= 1.5;
                break;
        }

        switch (mc.gameSettings.guiScale) {
            case 0:
                GlStateManager.scale(0.5, 0.5, 0.5);
                break;
            case 1:
                GlStateManager.scale(2, 2, 2);
                break;
            case 3:
                GlStateManager.scale(0.6666666666666667, 0.6666666666666667, 0.6666666666666667);
                break;
        }

        Gui.drawRect(0, 0, scaledResolution.getScaledWidth() * 3, scaledResolution.getScaledHeight() * 3, new Color(0, 0, 0, 120).getRGB());

        for (NovolineTab tab : tabs) {
            tab.drawScreen(mouseCoordsX, mouseCoordsY);
            if (tab.dragging) {
                tab.setPosX(dragX + mouseCoordsX);
                tab.setPosY(dragY + mouseCoordsY);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        GL11.glPopMatrix();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int x = mouseX;
        int y = mouseY;
        switch (mc.gameSettings.guiScale) {
            case 0:
                x *= 2;
                y *= 2;
                break;
            case 1:
                x *= 0.5;
                y *= 0.5;
                break;
            case 3:
                x *= 1.5;
                y *= 1.5;
                break;
        }

        for (NovolineTab tab : tabs) {
            if (tab.isHovered(x, y) && mouseButton == 0) {
                if (!anyDragging()) {
                    tab.dragging = true;
                    dragX = (int) (tab.getPosX() - x);
                    dragY = (int) (tab.getPosY() - y);
                }
            }
            try {
                tab.mouseClicked(x, y, mouseButton);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean anyDragging() {
        for (NovolineTab tab : tabs) {
            if (tab.dragging) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(null);
            if (mc.currentScreen == null) {
                mc.setIngameFocus();
            }
        } else {
            for (NovolineTab tab : tabs) {
                tab.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            for (NovolineTab tab : tabs) {
                tab.dragging = false;
            }
        }

        int x = mouseX;
        int y = mouseY;
        switch (mc.gameSettings.guiScale) {
            case 0:
                x *= 2;
                y *= 2;
                break;
            case 1:
                x *= 0.5;
                y *= 0.5;
                break;
            case 3:
                x *= 1.5;
                y *= 1.5;
                break;
        }

        for (NovolineTab tab : tabs) {
            tab.mouseReleased(x, y, state);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

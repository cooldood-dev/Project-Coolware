package com.github.cooldood.screens.ClickGUI;

import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.impl.client.ClickGUIModule;
import com.github.cooldood.utils.client.KeybindHandler;
import com.github.cooldood.utils.client.ScreenUtil;
import com.github.cooldood.utils.render.EasingUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ModuleRenderer extends Module {
    /** Outer height of a module card including top/bottom gap */
    private final int MODULE_HEIGHT    = 16;
    /** Vertical gap between successive module cards */
    private final int MODULE_GAP       = 0;
    /** Text indent from the left edge of the panel */
    private final float MODULE_TEXT_X  = ClickGUIScreen.BASE_X + ClickGUIScreen.PANEL_PADDING + 8;

    // ─────────────────────────────────────────────────────────────────────────

    public static String moduleName(Module module) {
        String name = module.getAnnotation().name();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public void render(Module module, int mouseX, int mouseY) {
        boolean isEnabled = module.isEnabled();
        boolean isMouseOver = ScreenUtil.isMouseOver(
                ClickGUIScreen.BASE_X + ClickGUIScreen.PANEL_PADDING,
                ClickGUIScreen.BASE_Y,
                ClickGUIScreen.GUI_TAB_WIDTH - ClickGUIScreen.PANEL_PADDING * 2,
                MODULE_HEIGHT,
                mouseX, mouseY
        ) && ClickGUIScreen.categoryRenderer.currentDraggingCategory == null;

        // Hover tracking
        if (isMouseOver) {
            if (ClickGUIScreen.moduleHovered == null) {
                ClickGUIScreen.hoverTime = System.currentTimeMillis();
                ClickGUIScreen.moduleHovered = module;
            }
        } else if (ClickGUIScreen.moduleHovered == module) {
            ClickGUIScreen.moduleHovered = null;
        }

        // Background colour
        Color bg;
        if (isEnabled) {
            bg = ClickGUIScreen.COL_MODULE_HOVER;
        } else {
            bg = isMouseOver ? ClickGUIScreen.COL_MODULE_HOVER : ClickGUIScreen.COL_MODULE_BG;
        }

        // Draw module card
        ClickGUIScreen.drawModuleCard(MODULE_HEIGHT, bg);

        // Enabled accent bar on the left edge
        if (isEnabled) {
            float barX = ClickGUIScreen.BASE_X + ClickGUIScreen.PANEL_PADDING;
            RenderUtil.drawRect(barX, ClickGUIScreen.BASE_Y, 2, MODULE_HEIGHT, ClickGUIScreen.COL_ACCENT);
        }

        // Module name text
        String name = moduleName(module);
        float textY = ClickGUIScreen.BASE_Y + MODULE_HEIGHT / 2f
                - FontUtil.getFontHeight(ClickGUIScreen.fontSize) / 2f + 1f;

        Color textColor = isEnabled ? Color.WHITE : ClickGUIScreen.COL_TEXT_DIM;
        FontUtil.drawString(name, MODULE_TEXT_X, textY, ClickGUIScreen.fontSize, textColor, true);

        // Keybind label on the right
        String keybindName = KeybindHandler.listeningModule == module
                ? "[…]"
                : module.getKeybind() != -1 ? "[" + Keyboard.getKeyName(module.getKeybind()) + "]" : "";

        if (!keybindName.isEmpty()) {
            float kbW = FontUtil.getStringWidth(keybindName, ClickGUIScreen.fontSize - 1);
            float kbX = ClickGUIScreen.BASE_X + ClickGUIScreen.GUI_TAB_WIDTH
                    - ClickGUIScreen.PANEL_PADDING - 6 - kbW;
            FontUtil.drawString(keybindName, kbX, textY, ClickGUIScreen.fontSize - 1,
                    ClickGUIScreen.COL_TEXT_DIM, true);
        }

        // Open indicator dot
        if (module.isOpen()) {
            float dotX = ClickGUIScreen.BASE_X + ClickGUIScreen.GUI_TAB_WIDTH
                    - ClickGUIScreen.PANEL_PADDING - 8;
            float dotY = ClickGUIScreen.BASE_Y + MODULE_HEIGHT / 2f - 2;
            RenderUtil.drawRect(dotX, dotY, 4, 4, ClickGUIScreen.COL_ACCENT);
        }

        // Advance Y by card height + gap
        GL11.glTranslated(0, MODULE_HEIGHT + MODULE_GAP, 0);
    }

    public void handleMouse(Module module, int mouseX, int mouseY) {
        if (ScreenUtil.isMouseOver(
                ClickGUIScreen.BASE_X + ClickGUIScreen.PANEL_PADDING,
                ClickGUIScreen.BASE_Y,
                ClickGUIScreen.GUI_TAB_WIDTH - ClickGUIScreen.PANEL_PADDING * 2,
                22, mouseX, mouseY)) {

            switch (ClickGUIScreen.mouseButton) {
                case 0:
                    module.toggle();
                    break;
                case 1:
                    module.setOpen(!module.isOpen());
                    EasingUtil.addAnimation(
                            module.getUniqueKey(""),
                            module.isOpen() ? ClickGUIModule.openAnimationLength : ClickGUIModule.closeAnimationLength,
                            module.isOpen(),
                            module.isOpen() ? ClickGUIModule.openAnimation : ClickGUIModule.closeAnimation
                    );
                    break;
                case 2:
                    KeybindHandler.listeningModule = module;
                    break;
            }
            ClickGUIScreen.mouseButton = -1;
        }
    }

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}
}

package com.github.cooldood.screens.ClickGUI;

import com.github.cooldood.modules.SubCategory;
import com.github.cooldood.modules.SubModule;
import com.github.cooldood.utils.client.ScreenUtil;
import com.github.cooldood.utils.render.FontUtil;

import java.awt.*;

public abstract class SubModuleRenderer {
    /** Row height for submodule rows */
    public final int SUBMODULE_HEIGHT = 18;

    /** Effective left X for the inner sub-module card area */
    public float getSubModuleCardX() {
        return ClickGUIScreen.BASE_X + ClickGUIScreen.PANEL_PADDING + 4;
    }

    /** Effective width for the inner sub-module card */
    public float getSubModuleCardW() {
        return ClickGUIScreen.GUI_TAB_WIDTH - (ClickGUIScreen.PANEL_PADDING + 4) * 2;
    }

    /** Text X — left-aligned inside the sub-module card */
    public float getSubModuleTextX() {
        return getSubModuleCardX() + 7;
    }

    /** Text Y — vertically centred in the row */
    public float getSubModuleTextY() {
        return ClickGUIScreen.BASE_Y + SUBMODULE_HEIGHT / 2f
                - FontUtil.getFontHeight(ClickGUIScreen.fontSize) / 2f;
    }

    public static void handle(int mouseX, int mouseY, SubModule subModule) {
        Class<?> fieldType = subModule.getField().getType();

        if (fieldType == boolean.class)       ClickGUIScreen.booleanSubModuleRenderer.render(mouseX, mouseY, subModule);
        else if (fieldType.isEnum())          ClickGUIScreen.enumSubModuleRenderer.render(mouseX, mouseY, subModule);
        else if (fieldType == Color.class)    ClickGUIScreen.colourSubModuleRenderer.render(mouseX, mouseY, subModule);
        else if (fieldType == SubCategory.class) ClickGUIScreen.subCategoryRenderer.render(mouseX, mouseY, subModule);
        else                                  ClickGUIScreen.sliderSubModuleRenderer.render(mouseX, mouseY, subModule);
    }

    public abstract void handleMouse(int mouseX, int mouseY, SubModule subModule);

    public void render(int mouseX, int mouseY, SubModule subModule) {
        this.handleMouse(mouseX, mouseY, subModule);

        boolean isHovered = ScreenUtil.isMouseOver(
                ClickGUIScreen.BASE_X,
                ClickGUIScreen.BASE_Y,
                ClickGUIScreen.GUI_TAB_WIDTH,
                SUBMODULE_HEIGHT, mouseX, mouseY);

        if (ClickGUIScreen.subModuleHovered == null && ClickGUIScreen.moduleHovered == null && isHovered) {
            ClickGUIScreen.subModuleHovered = subModule;
            ClickGUIScreen.hoverTime = System.currentTimeMillis();
        } else if (ClickGUIScreen.subModuleHovered == subModule && !isHovered) {
            ClickGUIScreen.subModuleHovered = null;
        }
    }
}

package com.github.cooldood.screens.ClickGUI.SubModuleRenderers;

import com.github.cooldood.modules.SubCategory;
import com.github.cooldood.modules.SubModule;
import com.github.cooldood.modules.impl.client.ClickGUIModule;
import com.github.cooldood.screens.ClickGUI.ClickGUIScreen;
import com.github.cooldood.screens.ClickGUI.SubModuleRenderer;
import com.github.cooldood.utils.client.ScreenUtil;
import com.github.cooldood.utils.render.EasingUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class SubCategoryRenderer extends SubModuleRenderer {
    /** Height of the divider row that serves as the sub-category header */
    public final int SUBCATEGORY_DIVIDER_HEIGHT = 16;

    @Override
    public void handleMouse(int mouseX, int mouseY, SubModule subModule) {
        if (ScreenUtil.isMouseOver(ClickGUIScreen.BASE_X, ClickGUIScreen.BASE_Y,
                ClickGUIScreen.GUI_TAB_WIDTH, SUBMODULE_HEIGHT, mouseX, mouseY)) {
            if (ClickGUIScreen.mouseButton == 0) {
                SubCategory subCategory = (SubCategory) subModule.get();
                EasingUtil.addAnimation(
                        subModule.getUniqueKey(),
                        !subCategory.open ? ClickGUIModule.openAnimationLength : ClickGUIModule.closeAnimationLength,
                        !subCategory.open,
                        !subCategory.open ? ClickGUIModule.openAnimation : ClickGUIModule.closeAnimation
                );
                subCategory.open = !subCategory.open;
            }
            ClickGUIScreen.mouseButton = -1;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, SubModule subModule) {
        super.render(mouseX, mouseY, subModule);

        SubCategory subCategory = (SubCategory) subModule.get();
        Color catColor = subModule.getParentModule().getAnnotation().category().color;

        // Draw a thin divider strip with the sub-category label centred
        float cardX = getSubModuleCardX();
        float cardW = getSubModuleCardW();

        // Divider background — slightly lighter than submod bg
        RenderUtil.drawRect(cardX, ClickGUIScreen.BASE_Y, cardW, SUBMODULE_HEIGHT,
                new Color(30, 30, 35, 255));

        // Left accent mark
        RenderUtil.drawRect(cardX, ClickGUIScreen.BASE_Y, 2, SUBMODULE_HEIGHT, ClickGUIScreen.COL_ACCENT);

        // Label centred
        String label = subModule.getAnnotation().name();
        float textW = FontUtil.getStringWidth(label, ClickGUIScreen.fontSize - 1);
        float textX = cardX + cardW / 2f - textW / 2f;
        float textY = ClickGUIScreen.BASE_Y + SUBMODULE_HEIGHT / 2f
                - FontUtil.getFontHeight(ClickGUIScreen.fontSize - 1) / 2f;
        FontUtil.drawString(label, textX, textY, ClickGUIScreen.fontSize - 1,
                ClickGUIScreen.COL_TEXT_DIM, true);

        // Chevron
        RenderUtil.drawArrow(cardX + cardW - 12, ClickGUIScreen.BASE_Y + SUBMODULE_HEIGHT / 2f - 2,
                6, 4, subCategory.open, 1, ClickGUIScreen.COL_TEXT_DIM);

        GL11.glTranslated(0, SUBMODULE_HEIGHT + 2, 0);
    }
}

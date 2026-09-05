package com.github.cooldood.screens.ClickGUI;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.impl.client.ClickGUIModule;
import com.github.cooldood.utils.client.ScreenUtil;
import com.github.cooldood.utils.render.EasingUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class CategoryRenderer {
    public final int CATEGORY_HEIGHT = 26;
    protected Category currentDraggingCategory = null;
    private float categoryDragStartX = -1;
    private float categoryDragStartY = -1;

    public void render(Category category) {
        // Draw the outer clay panel for just the header
        ClickGUIScreen.drawClayPanel(CATEGORY_HEIGHT, category.color);

        // Category icon/label — proper case style
        String name = category.name().replaceAll("_", " ");
        String categoryName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        float textH = FontUtil.getFontHeight(ClickGUIScreen.fontSize);
        float textX = ClickGUIScreen.BASE_X + 8f; // Left-aligned
        float textY = ClickGUIScreen.BASE_Y + CATEGORY_HEIGHT / 2f - textH / 2f + 1f;

        FontUtil.drawString(categoryName, textX, textY, ClickGUIScreen.fontSize, Color.WHITE, true);

        // Small arrow on the right indicating open/closed
        float arrowX = ClickGUIScreen.BASE_X + ClickGUIScreen.GUI_TAB_WIDTH - 14;
        float arrowY = ClickGUIScreen.BASE_Y + CATEGORY_HEIGHT / 2f - 2f;
        RenderUtil.drawArrow(arrowX, arrowY, 6, 4, category.shouldShow(), 1, ClickGUIScreen.COL_TEXT_DIM);

        GL11.glTranslated(0, CATEGORY_HEIGHT, 0);
    }

    public void handleMouse(Category category, int mouseX, int mouseY) {
        if (ScreenUtil.isMouseOver(ClickGUIScreen.BASE_X, ClickGUIScreen.BASE_Y,
                ClickGUIScreen.GUI_TAB_WIDTH, CATEGORY_HEIGHT, mouseX, mouseY)) {

            if (ClickGUIScreen.mouseButton == 0) {
                categoryDragStartX = mouseX - category.posX;
                categoryDragStartY = mouseY - category.posY;
                currentDraggingCategory = category;
            }
            if (ClickGUIScreen.mouseButton == 1) {
                category.open = !category.open;
                EasingUtil.addAnimation(
                        category.name(),
                        category.open ? ClickGUIModule.openAnimationLength : ClickGUIModule.closeAnimationLength,
                        category.open,
                        category.open ? ClickGUIModule.openAnimation : ClickGUIModule.closeAnimation
                );
            }
            ClickGUIScreen.mouseButton = -1;
        }

        if (currentDraggingCategory == category) {
            category.posX = mouseX - categoryDragStartX;
            category.posY = mouseY - categoryDragStartY;
        }
    }
}

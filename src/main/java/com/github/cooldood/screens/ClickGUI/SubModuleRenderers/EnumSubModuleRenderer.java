package com.github.cooldood.screens.ClickGUI.SubModuleRenderers;

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

public class EnumSubModuleRenderer extends SubModuleRenderer {
    @Override
    public void handleMouse(int mouseX, int mouseY, SubModule subModule) {
        if (ScreenUtil.isMouseOver(ClickGUIScreen.BASE_X, ClickGUIScreen.BASE_Y,
                ClickGUIScreen.GUI_TAB_WIDTH, SUBMODULE_HEIGHT, mouseX, mouseY)) {
            if (ClickGUIScreen.mouseButton == 0 || ClickGUIScreen.mouseButton == 1) {
                Enum<?> enumValue  = (Enum<?>) subModule.get();
                int     enumValues = subModule.getField().getType().getEnumConstants().length;
                int     next       = enumValue.ordinal() + (ClickGUIScreen.mouseButton == 0 ? 1 : -1);
                if (next < 0) next = enumValues - 1;
                subModule.set(subModule.getField().getType().getEnumConstants()[next % enumValues]);

                Enum<?> newEnumValue = (Enum<?>) subModule.get();

                EasingUtil.addAnimation(
                        (subModule.getUniqueKey() + enumValue.name()).toLowerCase(),
                        ClickGUIModule.closeAnimationLength, false, ClickGUIModule.closeAnimation);
                EasingUtil.addAnimation(
                        (subModule.getUniqueKey() + newEnumValue.name()).toLowerCase(),
                        ClickGUIModule.openAnimationLength, true, ClickGUIModule.openAnimation);
            }
            ClickGUIScreen.mouseButton = -1;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, SubModule subModule) {
        super.render(mouseX, mouseY, subModule);

        Color catColor = subModule.getParentModule().getAnnotation().category().color;

        ClickGUIScreen.drawSubModuleCard(SUBMODULE_HEIGHT);

        String enumValue = ((Enum<?>) subModule.get()).name().replace("_", " ");

        // Setting label
        FontUtil.drawString(subModule.getAnnotation().name(),
                getSubModuleTextX(), getSubModuleTextY(),
                ClickGUIScreen.fontSize, Color.WHITE, true);

        // Value chip on right
        float chipPadW = 4;
        float chipH    = FontUtil.getFontHeight(ClickGUIScreen.fontSize - 1) + 4;
        float valW     = FontUtil.getStringWidth(enumValue, ClickGUIScreen.fontSize - 1);
        float chipW    = valW + chipPadW * 2;
        float chipX    = ClickGUIScreen.BASE_X + ClickGUIScreen.GUI_TAB_WIDTH
                - ClickGUIScreen.PANEL_PADDING - 4 - chipW - 4;
        float chipY    = ClickGUIScreen.BASE_Y + SUBMODULE_HEIGHT / 2f - chipH / 2f;

        // Chip background
        RenderUtil.drawRect(chipX, chipY, chipW, chipH, 
                new Color(catColor.getRed(), catColor.getGreen(), catColor.getBlue(), 60));
        // Chip text
        FontUtil.drawString(enumValue, chipX + chipPadW, chipY + 2,
                ClickGUIScreen.fontSize - 1, catColor.brighter(), true);

        GL11.glTranslated(0, SUBMODULE_HEIGHT + 2, 0);
    }
}
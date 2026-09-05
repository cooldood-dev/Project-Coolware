package com.github.cooldood.screens.ClickGUI.SubModuleRenderers;

import com.github.cooldood.modules.SubModule;
import com.github.cooldood.screens.ClickGUI.ClickGUIScreen;
import com.github.cooldood.screens.ClickGUI.SubModuleRenderer;
import com.github.cooldood.utils.client.ScreenUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class SliderSubModuleRenderer extends SubModuleRenderer {
    private static final int SLIDER_H      = 4;
    private static final int SLIDER_HEIGHT = 28; // taller card to fit label + track

    @Override
    public void handleMouse(int mouseX, int mouseY, SubModule subModule) {
        if (ScreenUtil.isMouseOver(ClickGUIScreen.BASE_X, ClickGUIScreen.BASE_Y,
                ClickGUIScreen.GUI_TAB_WIDTH, SLIDER_HEIGHT, mouseX, mouseY)) {
            if (ClickGUIScreen.mouseButton == 0) ClickGUIScreen.currentSubModule = subModule;
            ClickGUIScreen.mouseButton = -1;
        }

        if (ClickGUIScreen.currentSubModule == subModule) {
            int[] mousePos = ScreenUtil.fixMousePos(mouseX, mouseY);
            float cardX = ClickGUIScreen.BASE_X + ClickGUIScreen.PANEL_PADDING + 4;
            float cardW = ClickGUIScreen.GUI_TAB_WIDTH - (ClickGUIScreen.PANEL_PADDING + 4) * 2;

            double value = mousePos[0] - cardX;
            value /= cardW;
            value *= (subModule.getAnnotation().max() - subModule.getAnnotation().min());
            value += subModule.getAnnotation().min();
            value = MathHelper.clamp_double(value, subModule.getAnnotation().min(), subModule.getAnnotation().max());
            subModule.set(value);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, SubModule subModule) {
        super.render(mouseX, mouseY, subModule);

        Color catColor = subModule.getParentModule().getAnnotation().category().color;
        float cardX = getSubModuleCardX();
        float cardW = getSubModuleCardW();

        // Draw taller sub-module clay card background
        ClickGUIScreen.drawSubModuleCard(SLIDER_HEIGHT);

        // Label on top row
        float labelY = ClickGUIScreen.BASE_Y + 5;
        FontUtil.drawString(subModule.getAnnotation().name(),
                cardX + 4, labelY,
                ClickGUIScreen.fontSize, Color.WHITE, true);

        // Value label on right of the top row
        String val = subModule.get().toString();
        float valW = FontUtil.getStringWidth(val, ClickGUIScreen.fontSize - 1);
        FontUtil.drawString(val,
                cardX + cardW - valW - 4, labelY,
                ClickGUIScreen.fontSize - 1, ClickGUIScreen.COL_TEXT_DIM, true);

        // Slider track in bottom half of card
        float trackY = ClickGUIScreen.BASE_Y + SLIDER_HEIGHT - SLIDER_H - 7;
        RenderUtil.drawRect(cardX + 2, trackY, cardW - 4, SLIDER_H, 
                new Color(30, 30, 35, 255));

        // Filled portion
        double percent = (Double.parseDouble(subModule.get().toString()) - subModule.getAnnotation().min())
                / (subModule.getAnnotation().max() - subModule.getAnnotation().min());
        float fillW = (float) (percent * (cardW - 4));
        if (fillW > 0) {
            RenderUtil.drawRect(cardX + 2, trackY, fillW, SLIDER_H, ClickGUIScreen.COL_ACCENT);
        }

        // Thumb dot
        float thumbX = cardX + 2 + fillW - (SLIDER_H + 2) / 2f;
        thumbX = MathHelper.clamp_float(thumbX, cardX + 2, cardX + cardW - 4 - SLIDER_H - 2);
        RenderUtil.drawRect(thumbX, trackY - 1, SLIDER_H + 2, SLIDER_H + 2, Color.WHITE);

        GL11.glTranslated(0, SLIDER_HEIGHT + 2, 0);
    }
}

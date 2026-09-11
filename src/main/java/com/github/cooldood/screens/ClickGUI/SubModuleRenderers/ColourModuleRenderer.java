package com.github.cooldood.screens.ClickGUI.SubModuleRenderers;

import com.github.cooldood.modules.SubModule;
import com.github.cooldood.modules.impl.client.ClickGUIModule;
import com.github.cooldood.screens.ClickGUI.ClickGUIScreen;
import com.github.cooldood.screens.ClickGUI.SubModuleRenderer;
import com.github.cooldood.utils.client.ScreenUtil;
import com.github.cooldood.utils.render.EasingUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ColourModuleRenderer extends SubModuleRenderer {
    private static final int DISPLAY_COLOR_SIZE = 15;
    private static final int BOTTOM_GAP         = 8;
    private static final int COLOUR_SQUARE_SIZE = 96;
    private static final int SLIDERS_HEIGHT     = 10;

    private int getColourSquareX() {
        return (int)(ClickGUIScreen.BASE_X + (ClickGUIScreen.GUI_TAB_WIDTH / 2f) - (COLOUR_SQUARE_SIZE / 2f));
    }

    private int getColourSquareY() {
        return (int)(ClickGUIScreen.BASE_Y + SUBMODULE_HEIGHT + BOTTOM_GAP);
    }

    private final Color[] HSLcolours = {
            new Color(255, 0,   0),
            new Color(255, 255, 0),
            new Color(0,   255, 0),
            new Color(0,   255, 255),
            new Color(0,   0,   255),
            new Color(255, 0,   255),
            new Color(255, 0,   0),
    };

    @Override
    public void handleMouse(int mouseX, int mouseY, SubModule subModule) {
        if (ScreenUtil.isMouseOver(ClickGUIScreen.BASE_X, ClickGUIScreen.BASE_Y,
                ClickGUIScreen.GUI_TAB_WIDTH, SUBMODULE_HEIGHT, mouseX, mouseY)) {
            if (ClickGUIScreen.mouseButton == 0) {
                boolean currentState = subModule.colorSettingValues.open;
                subModule.colorSettingValues.open = !currentState;
                if (!currentState) {
                    Color currentColour = (Color) subModule.get();
                    float[] hsbValues = Color.RGBtoHSB(currentColour.getRed(), currentColour.getGreen(), currentColour.getBlue(), null);
                    subModule.colorSettingValues.mX       = (int)(hsbValues[1] * COLOUR_SQUARE_SIZE);
                    subModule.colorSettingValues.mY       = COLOUR_SQUARE_SIZE - (int)(hsbValues[2] * COLOUR_SQUARE_SIZE);
                    subModule.colorSettingValues.hueValue = hsbValues[0];
                }
                EasingUtil.addAnimation(
                        subModule.getUniqueKey(),
                        !currentState ? ClickGUIModule.openAnimationLength : ClickGUIModule.closeAnimationLength,
                        !currentState,
                        !currentState ? ClickGUIModule.openAnimation : ClickGUIModule.closeAnimation
                );
                ClickGUIScreen.mouseButton = -1;
                return;
            }
        }

        if (!subModule.colorSettingValues.open) return;

        Color currentColour = (Color) subModule.get();
        int[] mousePos = ScreenUtil.fixMousePos(mouseX, mouseY);
        int csX = getColourSquareX();
        int csY = getColourSquareY();

        if (ScreenUtil.isMouseOver(csX, csY, COLOUR_SQUARE_SIZE, COLOUR_SQUARE_SIZE, mouseX, mouseY)) {
            if (ClickGUIScreen.mouseButton == 0) ClickGUIScreen.currentSubModule = subModule;
            ClickGUIScreen.mouseButton = -1;
        }

        if (ClickGUIScreen.currentSubModule == subModule) {
            float hue        = subModule.colorSettingValues.hueValue;
            float saturation = MathHelper.clamp_float((float)(mousePos[0] - csX) / COLOUR_SQUARE_SIZE, 0, 1);
            float brightness = MathHelper.clamp_float(1 - ((float)(mousePos[1] - csY) / COLOUR_SQUARE_SIZE), 0, 1);
            Color newColour  = Color.getHSBColor(hue, saturation, brightness);
            newColour = new Color(newColour.getRed(), newColour.getGreen(), newColour.getBlue(), currentColour.getAlpha());
            subModule.set(newColour);
            subModule.colorSettingValues.mX = MathHelper.clamp_int(mousePos[0] - csX, 0, COLOUR_SQUARE_SIZE);
            subModule.colorSettingValues.mY = MathHelper.clamp_int(mousePos[1] - ClickGUIScreen.BASE_Y - SUBMODULE_HEIGHT - BOTTOM_GAP, 0, COLOUR_SQUARE_SIZE);
            return;
        }

        if (!ClickGUIScreen.leftMouseDown) return;

        // Hue slider
        if (ScreenUtil.isMouseOver(csX, csY + COLOUR_SQUARE_SIZE + BOTTOM_GAP, COLOUR_SQUARE_SIZE, SLIDERS_HEIGHT, mouseX, mouseY)) {
            float hue = MathHelper.clamp_float((float)(mousePos[0] - csX) / COLOUR_SQUARE_SIZE, 0, 1);
            subModule.colorSettingValues.hueValue = hue;
            float[] hsb  = Color.RGBtoHSB(currentColour.getRed(), currentColour.getGreen(), currentColour.getBlue(), null);
            Color colour = Color.getHSBColor(hue, hsb[1], hsb[2]);
            colour = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), currentColour.getAlpha());
            subModule.set(colour);
        }
        // Alpha slider
        else if (ScreenUtil.isMouseOver(csX, csY + COLOUR_SQUARE_SIZE + BOTTOM_GAP * 2 + SLIDERS_HEIGHT, COLOUR_SQUARE_SIZE, SLIDERS_HEIGHT, mouseX, mouseY)) {
            int opacity = (int)(MathHelper.clamp_float((float)(mousePos[0] - csX) / COLOUR_SQUARE_SIZE, 0, 1) * 255);
            subModule.set(new Color(currentColour.getRed(), currentColour.getGreen(), currentColour.getBlue(), opacity));
        }
    }

    @Override
    public void render(int mouseX, int mouseY, SubModule subModule) {
        super.render(mouseX, mouseY, subModule);

        Color currentColour = (Color) subModule.get();

        // Row card
        ClickGUIScreen.drawSubModuleCard(SUBMODULE_HEIGHT);

        // Label
        FontUtil.drawString(subModule.getAnnotation().name(),
                getSubModuleTextX(), getSubModuleTextY(),
                ClickGUIScreen.fontSize, Color.WHITE, true);

        // Colour swatch (square)
        float labelW = FontUtil.getStringWidth(subModule.getAnnotation().name(), ClickGUIScreen.fontSize);
        float swatchX = getSubModuleTextX() + labelW + 6;
        float swatchY = ClickGUIScreen.BASE_Y + SUBMODULE_HEIGHT / 2f - DISPLAY_COLOR_SIZE / 2f;
        RenderUtil.drawRect(swatchX, swatchY, DISPLAY_COLOR_SIZE, DISPLAY_COLOR_SIZE, currentColour);

        GL11.glTranslated(0, SUBMODULE_HEIGHT + 2, 0);

        // ── Expanded colour picker ──────────────────────────────────────────
        double animProgress = EasingUtil.getAnimation(subModule.getUniqueKey());
        boolean open = subModule.colorSettingValues.open || animProgress != -1;
        if (!open) return;

        int expandedH = COLOUR_SQUARE_SIZE + BOTTOM_GAP * 4 + SLIDERS_HEIGHT * 2;
        // Background card for the picker
        ClickGUIScreen.drawSubModuleCard(expandedH);

        if (animProgress != -1) GL11.glScaled(1, animProgress, 1);

        int csX = getColourSquareX();
        int csY = getColourSquareY() - (int)((double)SUBMODULE_HEIGHT);

        // Saturation/Brightness square
        Color hueColour = Color.getHSBColor(subModule.colorSettingValues.hueValue, 1, 1);
        RenderUtil.drawGradientLR(csX, BOTTOM_GAP, COLOUR_SQUARE_SIZE, COLOUR_SQUARE_SIZE, Color.WHITE, hueColour);
        RenderUtil.drawGradientTB(csX, BOTTOM_GAP, COLOUR_SQUARE_SIZE, COLOUR_SQUARE_SIZE, new Color(0, 0, 0, 0), Color.BLACK);
        RenderUtil.drawRectOutline(csX - 1, BOTTOM_GAP - 1, COLOUR_SQUARE_SIZE + 2, COLOUR_SQUARE_SIZE + 2, 1, ClickGUIScreen.COL_ACCENT);

        // Crosshair
        RenderUtil.drawRectOutline(
                csX + subModule.colorSettingValues.mX - 3,
                ClickGUIScreen.BASE_Y + BOTTOM_GAP + subModule.colorSettingValues.mY - 3,
                6, 6, 1, Color.WHITE);

        GL11.glTranslated(0, COLOUR_SQUARE_SIZE + BOTTOM_GAP * 2, 0);

        // Hue rainbow slider
        for (int i = 0; i < HSLcolours.length - 1; i++) {
            float w = (float) COLOUR_SQUARE_SIZE / (HSLcolours.length - 1);
            RenderUtil.drawGradientLR(csX + w * i, 0, w, SLIDERS_HEIGHT, HSLcolours[i], HSLcolours[i + 1]);
        }
        RenderUtil.drawRectOutline(csX, 0, COLOUR_SQUARE_SIZE, SLIDERS_HEIGHT, 1, ClickGUIScreen.COL_ACCENT);

        int hueThumbX = MathHelper.clamp_int((int)(subModule.colorSettingValues.hueValue * COLOUR_SQUARE_SIZE) - 3, 0, COLOUR_SQUARE_SIZE - 6);
        RenderUtil.drawRect(csX + hueThumbX, -1, 6, SLIDERS_HEIGHT + 2, Color.WHITE);

        GL11.glTranslated(0, SLIDERS_HEIGHT + BOTTOM_GAP, 0);

        // Alpha slider
        RenderUtil.drawGradientLR(csX, 0, COLOUR_SQUARE_SIZE, SLIDERS_HEIGHT, new Color(0, 0, 0, 0), Color.WHITE);
        RenderUtil.drawRectOutline(csX, 0, COLOUR_SQUARE_SIZE, SLIDERS_HEIGHT, 1, ClickGUIScreen.COL_ACCENT);

        int alphaThumbX = MathHelper.clamp_int((int)((currentColour.getAlpha() / 255f) * COLOUR_SQUARE_SIZE) - 3, 0, COLOUR_SQUARE_SIZE - 6);
        RenderUtil.drawRect(csX + alphaThumbX, -1, 6, SLIDERS_HEIGHT + 2, Color.WHITE);

        GL11.glTranslated(0, SLIDERS_HEIGHT + BOTTOM_GAP, 0);

        if (animProgress != -1) GL11.glScaled(1, 1 / animProgress, 1);
    }
}
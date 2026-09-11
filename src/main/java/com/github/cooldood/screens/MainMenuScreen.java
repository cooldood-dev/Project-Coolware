package com.github.cooldood.screens;

import com.github.cooldood.Main;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.ScreenUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;

import java.awt.*;
import java.io.IOException;

public class MainMenuScreen extends GuiScreen {
    String[] strings = {"singleplayer", "multiplayer", "settings", "alts"};

    @Override
    public void drawScreen(int mX, int mY, float partialTicks) {
        drawBackground();
        drawButtons(mX, mY);

        leftMousePressed = false;

        super.drawScreen(mX, mY, partialTicks);
    }

    public void drawButtons(int mX, int mY) {
        int screenWidth = C.res().getScaledWidth();
        int screenHeight = C.res().getScaledHeight();

        float w = 300;
        float h = 220;
        float x = screenWidth/2f - w/2;
        float y = screenHeight/2f - h/2;


        String clientNameText = Main.MOD_NAME + " " + Main.MOD_VERSION;
        float textX = screenWidth/2f;
        float textY = y+10;

        RenderUtil.drawBlurRect(x, y, w, h, 8); // stronger blur
        RenderUtil.drawRoundedRect(x, y, w, h, 8, new Color(40, 40, 45, 120)); // gray translucent
        RenderUtil.drawRoundedRectOutline(x, y, w, h, 8, 1, new Color(255, 255, 255, 40)); // soft white edge
        FontUtil.drawCenteredString(clientNameText, textX, textY, 30, new Color(240, 240, 240, 255), true);

        for (int i = 0; i < strings.length; i++) {
            String string = strings[i].substring(0, 1).toUpperCase() + strings[i].substring(1); // proper case

            float newW = w / 1.3f;
            float newH = h/8;
            float newX = (x + w/2f) - newW/2f;
            float newY = y+newH*(i+1) + i * 10 + 30;

            float newTextX = (newX + newW/2f);
            float newTextY = newY+(newH/2) - FontUtil.getFontHeight(18)/2f;

            boolean hovered = ScreenUtil.isMouseOver(newX, newY, newW, newH, mX, mY);

            Color stringColor = hovered ? new Color(255,255,255,255) : new Color(220,220,220,200);

            // Glassmorphic button
            RenderUtil.drawRoundedRect(newX, newY, newW, newH, 4, hovered ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 20));
            RenderUtil.drawRoundedRectOutline(newX, newY, newW, newH, 4, 1, new Color(255, 255, 255, 60));

            FontUtil.drawCenteredString(string, newTextX, newTextY, 18, stringColor, true);

            if (hovered) {
                if (leftMousePressed) {
                    switch (i) {
                        case 0:
                            C.mc.displayGuiScreen(new GuiSelectWorld(this));
                            break;
                        case 1:
                            C.mc.displayGuiScreen(new GuiMultiplayer(this));
                            break;
                        case 2:
                            C.mc.displayGuiScreen(new GuiOptions(this, C.mc.gameSettings));
                            break;
                        case 3:
                            C.mc.displayGuiScreen(new AltManagerScreen());
                            break;
                    }
                }
            }
        }
    }

    public static void drawBackground() {
        int w = C.res().getScaledWidth();
        int h = C.res().getScaledHeight();
        // Deep dark gradient — top slightly lighter, bottom darker
        RenderUtil.drawGradientTB(0, 0, w, h, new Color(22, 22, 28, 255), new Color(12, 12, 16, 255));
        // Subtle vignette — darker edges
        RenderUtil.drawGradientLR(0, 0, w / 3f, h, new Color(0, 0, 0, 80), new Color(0, 0, 0, 0));
        RenderUtil.drawGradientLR(w - w / 3f, 0, w / 3f, h, new Color(0, 0, 0, 0), new Color(0, 0, 0, 80));
        RenderUtil.drawGradientTB(0, 0, w, h / 4f, new Color(0, 0, 0, 60), new Color(0, 0, 0, 0));
        RenderUtil.drawGradientTB(0, h - h / 4f, w, h / 4f, new Color(0, 0, 0, 0), new Color(0, 0, 0, 60));
    }


    public static boolean leftMousePressed = false;

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);

        if (button == 0) leftMousePressed = true;
    }

    @Override
    protected void mouseReleased(int x, int y, int button) {
        super.mouseReleased(x, y, button);

        if (button == 0) leftMousePressed = false;
    }
}
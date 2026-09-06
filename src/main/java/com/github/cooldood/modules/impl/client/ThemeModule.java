package com.github.cooldood.modules.impl.client;

import com.github.cooldood.modules.*;
import com.github.cooldood.utils.render.FontUtil;

import java.awt.*;

@RegisterModule(
        name = "Theme",
        description = "Provides Theme functionality for the client.",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class ThemeModule extends Module {

    public static boolean globalFont = false;
    public static int minecraftFontSize = 10;

    public static boolean shouldUseCustomFont() {
        return false;
    }

    @RegisterSubModule(name = "Primary Color", description = "Main accent color for HUD, ClickGUI, and highlights")
    public static Color primaryColor = new Color(181, 166, 242);

    @RegisterSubModule(name = "Secondary Color", description = "Secondary accent color for gradients and fades")
    public static Color secondaryColor = new Color(130, 200, 255);

    @RegisterSubModule(name = "Color Mode", description = "Color palette style")
    public static ColorMode colorMode = ColorMode.Custom;
    public enum ColorMode {
        Custom, Theme_Fade, Rainbow, Pastel
    }

    @RegisterSubModule(name = "Gradient Speed", min = 0.5, max = 5.0, increment = 0.5)
    public static float gradientSpeed = 1.5f;

    @RegisterSubModule(name = "Panel Background", description = "Background color for ClickGUI and HUD panels")
    public static Color panelBackground = new Color(15, 15, 18, 190);

    public static Color[] getThemeColours() {
        switch (colorMode) {
            case Theme_Fade:
                return new Color[]{primaryColor, secondaryColor};
            case Rainbow:
                return new Color[]{
                        new Color(255, 80, 80),
                        new Color(255, 180, 50),
                        new Color(255, 255, 80),
                        new Color(80, 255, 80),
                        new Color(80, 180, 255),
                        new Color(180, 80, 255)
                };
            case Pastel:
                return new Color[]{
                        new Color(255, 179, 186),
                        new Color(255, 223, 186),
                        new Color(255, 255, 186),
                        new Color(186, 255, 201),
                        new Color(186, 225, 255)
                };
            case Custom:
            default:
                return new Color[]{primaryColor, primaryColor};
        }
    }

    public static Color getAccentColor() {
        if (colorMode == ColorMode.Rainbow || colorMode == ColorMode.Theme_Fade || colorMode == ColorMode.Pastel) {
            return com.github.cooldood.utils.render.RenderUtil.getColorsFade(0, getThemeColours(), gradientSpeed);
        }
        return primaryColor;
    }

    public static Color getAccentColor(double offset) {
        if (colorMode == ColorMode.Rainbow || colorMode == ColorMode.Theme_Fade || colorMode == ColorMode.Pastel) {
            return com.github.cooldood.utils.render.RenderUtil.getColorsFade(offset, getThemeColours(), gradientSpeed);
        }
        return primaryColor;
    }

    @Override
    protected void onEnable() {
        FontUtil.setCurrentFont(FontUtil.Fonts.DM_Sans_Bold);
    }

    @Override
    protected void onDisable() {
        // keep always enabled
        ModuleManager.getModule(ThemeModule.class).setEnabled(true);
    }
}

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

    @RegisterSubModule(name = "Primary Color")
    public static Color primaryColor = new Color(181, 166, 242);

    public static Color[] getThemeColours() {
        return new Color[]{primaryColor, primaryColor};
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

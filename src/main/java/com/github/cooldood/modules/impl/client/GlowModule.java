package com.github.cooldood.modules.impl.client;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;

import java.awt.Color;

@RegisterModule(
        name = "Glow",
        description = "Adds a customizable glow / bloom effect to UI cards and visuals.",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class GlowModule extends Module {

    @RegisterSubModule(name = "Radius", min = 1, max = 10, increment = 1)
    public static double radius = 3;

    @RegisterSubModule(name = "Intensity", min = 1, max = 5, increment = 1)
    public static double intensity = 2;

    @RegisterSubModule(name = "Custom Color")
    public static boolean customColor = false;

    @RegisterSubModule(name = "Glow Color", parent = "Custom Color")
    public static Color glowColor = new Color(181, 166, 242, 180);

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}
}

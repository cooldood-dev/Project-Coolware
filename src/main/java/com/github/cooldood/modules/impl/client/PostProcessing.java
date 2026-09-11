package com.github.cooldood.modules.impl.client;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;

@RegisterModule(
        enabledByDefault = true,
        name = "PostProcessing",
        description = "Adds blur and bloom effects.",
        category = Category.CLIENT
)
public class PostProcessing extends Module {
    @com.github.cooldood.modules.RegisterSubModule(name = "Iterations", min = 1, max = 5, increment = 1)
    public static double iterations = 2;

    @com.github.cooldood.modules.RegisterSubModule(name = "Offset", min = 1, max = 6, increment = 1)
    public static double offset = 2;
    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}

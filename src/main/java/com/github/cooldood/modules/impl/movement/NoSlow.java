package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.PlayerUtil;

@RegisterModule(
        name = "No Slow",
        description = "Provides No Slow functionality for the client.",
        category = Category.MOVEMENT,
        dangerous = true
)
public class NoSlow extends Module {
    public static boolean shouldSlowDown() {
        return !ModuleManager.isEnabled(NoSlow.class) && PlayerUtil.isUsingItem();
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

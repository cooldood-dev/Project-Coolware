package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.modules.*;

@RegisterModule(
        name = "No Jump Delay",
        description = "Provides No Jump Delay functionality for the client.",
        category = Category.MOVEMENT
)
public class NoJumpDelay extends Module {
    @RegisterSubModule(name = "Delay Ticks", max = 20)
    public static int delay = 1;

    public static int getDelay() {
        return ModuleManager.isEnabled(NoJumpDelay.class) ? delay : 10;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

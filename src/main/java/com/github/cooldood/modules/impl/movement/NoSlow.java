package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.impl.combat.KillAura;
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
        if (KillAura.shouldPreventServerBlock()) {
            return false;
        }
        return !ModuleManager.isEnabled(NoSlow.class) && ((C.p() != null && C.p().isUsingItem()) || com.github.cooldood.modules.impl.combat.Watchdog.isModuleBlocking());
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

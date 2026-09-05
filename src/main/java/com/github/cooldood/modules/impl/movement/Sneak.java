package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MovementInputEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.client.C;

@RegisterModule(
        name = "Sneak",
        description = "Automatically sneaks for you.",
        category = Category.MOVEMENT,
        dangerous = true
)
public class Sneak extends Module {
    @SubscribeEvent
    public static void onMovementInputEvent(MovementInputEvent event) {
        if (ModuleManager.isEnabled(Sneak.class) && C.p().isSneaking()) {
            event.movementInput.moveStrafe /= 0.3f;
            event.movementInput.moveForward /= 0.3f;
        }
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

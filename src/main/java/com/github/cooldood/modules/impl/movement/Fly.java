package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MoveFlyingEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.modules.impl.render.Freecam;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.KeybindHandler;

@RegisterModule(
        name = "Fly",
        description = "Allows you to fly through the air.",
        category = Category.MOVEMENT,
        dangerous = true
)
public class Fly extends Module {
    @RegisterSubModule(name = "Horizontal Speed", max = 10)
    public static float horizontalSpeed = 2;
    @RegisterSubModule(name = "Vertical Speed", max = 10)
    public static float verticalSpeed = 2;

    @SubscribeEvent
    public static void onMoveFlying(MoveFlyingEvent event) {
        C.p().setVelocity(0,0,0);

        if (ModuleManager.isEnabled(Freecam.class) || C.mc.currentScreen != null) return;

        event.friction = horizontalSpeed;
        C.p().motionY +=
                (KeybindHandler.isKeyDown(C.mc.gameSettings.keyBindJump) ? verticalSpeed : 0) +
                        (KeybindHandler.isKeyDown(C.mc.gameSettings.keyBindSneak) ? -verticalSpeed : 0);
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MoveFlyingEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;

@RegisterModule(
        name = "Speed",
        description = "Increases your movement speed.",
        category = Category.MOVEMENT,
        dangerous = true
)
public class Speed extends Module {
    @RegisterSubModule(name = "Speed", max = 5)
    public static float speed = 2;

    @SubscribeEvent
    public static void onMoveFlying(MoveFlyingEvent event) {
        C.p().setVelocity(0,C.p().motionY,0);
        event.friction = speed;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

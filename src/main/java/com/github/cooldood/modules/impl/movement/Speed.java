package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MoveFlyingEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
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

    @RegisterSubModule(name = "Mode")
    public static SpeedMode mode = SpeedMode.Vanilla;
    public enum SpeedMode {
        Vanilla, Legit
    }

    @RegisterSubModule(name = "Speed", max = 5, parent = "Mode", modeParentString = "Vanilla")
    public static float speed = 2;

    @SubscribeEvent
    public static void onMoveFlying(MoveFlyingEvent event) {
        if (mode != SpeedMode.Vanilla) return;
        C.p().setVelocity(0, C.p().motionY, 0);
        event.friction = speed;
    }

    @SubscribeEvent
    public static void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mode != SpeedMode.Legit) return;
        if (C.p().onGround && (C.p().moveForward != 0 || C.p().moveStrafing != 0)) {
            C.p().jump();
        }
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.bridge.net.minecraft.client.settings.KeyBindingBridge;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.KeyPressedEvent;
import com.github.cooldood.events.impl.MovementInputEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.utils.client.C;

@RegisterModule(
        name = "Sprint",
        description = "",
        category = Category.MOVEMENT
)
public class Sprint extends Module {
    @RegisterSubModule(name = "Mode")
    public static Mode mode = Mode.Legit;

    public enum Mode {
        Legit,
        Toggle_Sprint,
        Omni_Sprint,
    }

    public static boolean shouldOmniSprint() {
        return ModuleManager.isEnabled(Sprint.class) && mode == Mode.Omni_Sprint;
    }

    private static boolean toggled = false;

    @SubscribeEvent
    public static void setSprinting(MovementInputEvent event) {
        switch (mode) {
            case Legit:
                KeyBindingBridge.from(C.mc.gameSettings.keyBindSprint).bridge$setDown(true);
                break;
            case Toggle_Sprint:
                KeyBindingBridge.from(C.mc.gameSettings.keyBindSprint).bridge$setDown(toggled);
                break;
        }
    }

    @SubscribeEvent
    public static void setToggleSprint(KeyPressedEvent event) {
        if (!event.pressed || event.keyCode != C.mc.gameSettings.keyBindSprint.getKeyCode() || mode != Mode.Toggle_Sprint) return;

        toggled = !toggled;
        if (!toggled) C.p().setSprinting(false);
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

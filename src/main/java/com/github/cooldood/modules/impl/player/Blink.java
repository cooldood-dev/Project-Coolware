package com.github.cooldood.modules.impl.player;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.ClientTickEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.BlinkUtil;

@RegisterModule(
        name = "Blink",
        description = "Suspends packets to simulate teleportation.",
        category = Category.PLAYER
)
public class Blink extends Module {
    @RegisterSubModule(name = "Outgoing")
    public static boolean blinkOutgoing = true;

    @RegisterSubModule(name = "Incoming")
    public static boolean blinkIncoming = true;

    private static boolean wasBlinkingOut, wasBlinkingIn;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (!C.isInGame()) {
            ModuleManager.getModule(Blink.class).toggle();
            return;
        }

        if (wasBlinkingOut != blinkOutgoing) {
            if (blinkOutgoing) BlinkUtil.pushBlink(true, false);
            else BlinkUtil.popBlink(true, false);
        }
        if (wasBlinkingIn != blinkIncoming) {
            if (blinkIncoming) BlinkUtil.pushBlink(false, true);
            else BlinkUtil.popBlink(false, true);
        }

        wasBlinkingOut = blinkOutgoing; wasBlinkingIn = blinkIncoming;
    }

    @Override
    protected void onEnable() {
        if (!C.isInGame()) {
            ModuleManager.getModule(Blink.class).toggle();
            return;
        }

        wasBlinkingOut = blinkOutgoing;
        wasBlinkingIn = blinkIncoming;

        BlinkUtil.pushBlink(blinkOutgoing, blinkIncoming);
    }

    @Override
    protected void onDisable() {
        if (C.isInGame()) BlinkUtil.popBlink(blinkOutgoing, blinkIncoming);
    }
}

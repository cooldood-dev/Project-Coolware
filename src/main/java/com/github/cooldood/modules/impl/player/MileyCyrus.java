package com.github.cooldood.modules.impl.player;

import com.github.cooldood.bridge.net.minecraft.client.settings.KeyBindingBridge;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MotionEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.KeybindHandler;

@RegisterModule(
        name = "Miley Cyrus",
        description = "Provides Miley Cyrus functionality for the client.",
        category = Category.PLAYER
)
public class MileyCyrus extends Module {
    @RegisterSubModule(name = "Server Side", description = "Only other players can see your crouching" )
    public static boolean serverSide = false;

    @SubscribeEvent
    public static void onPlayerUpdateEvent(PlayerUpdateEvent event) {
        if (!serverSide)
            KeyBindingBridge.from(C.mc.gameSettings.keyBindSneak).bridge$setDown(C.p().ticksExisted % 2 == 0);
    }

    @SubscribeEvent
    public static void onPlayerMotionEvent(MotionEvent event) {
        if (serverSide)
            event.sneaking = C.p().ticksExisted % 2 == 0;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {
        KeyBindingBridge.from(C.mc.gameSettings.keyBindSneak).bridge$setDown(KeybindHandler.isKeyDown(C.mc.gameSettings.keyBindSneak));
    }
}

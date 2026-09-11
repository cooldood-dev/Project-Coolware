package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MotionEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.client.C;

@RegisterModule(
        name = "Full Bright",
        description = "Provides Full Bright functionality for the client.",
        category = Category.RENDER
)
public class FullBright extends Module {
    private static float oldGamma = 0;

    @Override
    protected void onEnable() {
        oldGamma = C.mc.gameSettings.gammaSetting;
    }

    @Override
    protected void onDisable() {
        C.mc.gameSettings.gammaSetting = oldGamma;
    }

    @SubscribeEvent
    public static void onMotionEvent(MotionEvent event) {
        C.mc.gameSettings.gammaSetting = 100;
    }
}

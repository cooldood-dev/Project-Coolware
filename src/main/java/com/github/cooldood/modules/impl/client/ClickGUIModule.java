package com.github.cooldood.modules.impl.client;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.ClientTickEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.screens.ClickGUI.ClickGUIScreen;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.ScreenUtil;
import org.lwjgl.input.Keyboard;
import com.github.cooldood.utils.render.EasingUtil;

@RegisterModule(
        name = "Click GUI",
        description = "Provides Click GUI functionality for the client.",
        category = Category.CLIENT,
        keybind = Keyboard.KEY_RSHIFT
)
public class ClickGUIModule extends Module {
    @RegisterSubModule(name = "Fancy Dragging")
    public static boolean fancyDragging = true;

    @RegisterSubModule(name = "Open", parent = "Animations")
    public static EasingUtil.EasingFunctions openAnimation = EasingUtil.EasingFunctions.Ease_In_Out_Sine;

    @RegisterSubModule(name = "Open Length", parent = "Animations", max = 1000, increment = 50)
    public static long openAnimationLength = 100;

    @RegisterSubModule(name = "Close", parent = "Animations")
    public static EasingUtil.EasingFunctions closeAnimation = EasingUtil.EasingFunctions.Ease_In_Out_Sine;

    @RegisterSubModule(name = "Close Length", parent = "Animations", max = 1000, increment = 50)
    public static long closeAnimationLength = 100;

    private static final ClickGUIScreen screen = new ClickGUIScreen();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (!(C.mc.currentScreen instanceof ClickGUIScreen)) {
            ModuleManager.getModule(ClickGUIModule.class).setEnabled(false);
        }
    }

    @Override
    protected void onEnable() {
        if (!C.isInGame()) {
            this.toggle();
            return;
        }
        ScreenUtil.setGuiToDisplay(screen);
    }

    @Override
    protected void onDisable() {
    }
}

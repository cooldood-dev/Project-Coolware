package com.github.cooldood.modules.impl.player;

import com.github.cooldood.bridge.net.minecraft.client.settings.KeyBindingBridge;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@RegisterModule(
        name = "Inventory Fix",
        description = "Provides Inventory Fix functionality for the client.",
        category = Category.PLAYER,
        enabledByDefault = true
)
public class InventoryFix extends Module {
    @RegisterSubModule(name = "Only Movement")
    public static boolean movementOnly = true;

    private static boolean wasInGUI = false;

    @SubscribeEvent
    public static void onPlayerUpdate(PlayerUpdateEvent event) {
        if (C.mc.currentScreen != null) {
            wasInGUI = true;
            return;
        }

        if (!wasInGUI) return;

        for (KeyBinding keyBinding : C.mc.gameSettings.keyBindings) {
            if (keyBinding == C.mc.gameSettings.keyBindInventory) continue;
            if (!keyBinding.getKeyCategory().equals("key.categories.movement") && movementOnly) continue;

            if (keyBinding.getKeyCode() < 0) KeyBindingBridge.from(keyBinding).bridge$setDown(Mouse.isButtonDown(keyBinding.getKeyCode() + 100));
            else KeyBindingBridge.from(keyBinding).bridge$setDown(Keyboard.isKeyDown(keyBinding.getKeyCode()));
        }

        wasInGUI = false;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

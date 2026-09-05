package com.github.cooldood.utils.client;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.KeyPressedEvent;
import com.github.cooldood.modules.Module;
import com.github.cooldood.utils.minecraft.ChatUtil;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeybindHandler {
    public static HashMap<Integer, List<Module>> keybindsMap = new HashMap<>();

    public static Module listeningModule = null;

    @SubscribeEvent
    public static void onKeyPressed(KeyPressedEvent event) {
        List<Module> modules = keybindsMap.get(event.keyCode);

        if (event.pressed) {
            if (listeningModule != null) {
                registerKeybind(listeningModule, event.keyCode);

                ChatUtil.prefixMessage("&cSuccessfully bound &f" + listeningModule.getAnnotation().name() + " &cto &f" + getCharacter(event.keyCode));

                listeningModule = null;
                return;
            }
        }

        if (modules != null) {
            for (Module module : modules) {
                if (module.holdKeybind) module.setEnabled(event.pressed);
                else if (event.pressed) module.toggle();
            }
        }

    }

    public static void registerKeybind(Module module, int keyCode) {
        removeKeybind(module);

        if (!keybindsMap.containsKey(keyCode))
            keybindsMap.put(keyCode, Stream.of(module).collect(Collectors.toList()));
        else
            keybindsMap.get(keyCode).add(module);

        module.setKeybind(keyCode);
    }

    public static void removeKeybind(Module module) {
        for (Map.Entry<Integer, List<Module>> entry : keybindsMap.entrySet()) {
            if (entry.getValue().contains(module)) {
                entry.getValue().remove(module);
                if (entry.getValue().isEmpty()) keybindsMap.remove(entry.getKey());

                break;
            }
        }

        module.setKeybind(-1);
    }

    public static String getCharacter(int keyCode) {
        return Keyboard.getKeyName(keyCode);
    }

    // https://github.com/cooldood/ScaleHackOpenSrc/issues/1
    // shoutout to legitphase for pushing the fix on the opensrc one when i was too lazy <3 https://github.com/cooldood/ScaleHackOpenSrc/commit/c8ac3e097ed41bfa489160db7bf16f86071067da
    public static boolean isKeyDown(KeyBinding keyBinding) {
        return keyBinding.getKeyCode() < 0
                ? Mouse.isButtonDown(keyBinding.getKeyCode() + 100)
                : Keyboard.isKeyDown(keyBinding.getKeyCode());
    }
}
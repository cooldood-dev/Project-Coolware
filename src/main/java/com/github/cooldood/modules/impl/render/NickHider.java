package com.github.cooldood.modules.impl.render;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.impl.player.Disabler;
import net.minecraft.client.Minecraft;

@RegisterModule(
        name = "Nick Hider",
        description = "Provides Nick Hider functionality for the client.",
        category = Category.RENDER
)
public class NickHider extends Module {

    public static String fixText(String text) {
        if (ModuleManager.isEnabled(NickHider.class))
            text = text.replaceAll(Minecraft.getMinecraft().getSession().getUsername(), "scale");
        if (Disabler.racismDisabler && ModuleManager.isEnabled(Disabler.class)) {
            // patches racism. donald trump should try this
            for (String word : Disabler.racistWords) {
                if (text.contains(word))
                    text = text.replaceAll(word, Disabler.censorRacism(word));
            }
        }

        return text;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}
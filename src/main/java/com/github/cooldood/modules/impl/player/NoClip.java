package com.github.cooldood.modules.impl.player;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.minecraft.PlayerUtil;

@RegisterModule(
        name = "No Clip",
        description = "Provides No Clip functionality for the client.",
        category = Category.PLAYER
)
public class NoClip extends Module {
    @Override
    protected void onEnable() {
        PlayerUtil.noClip = true;
        PlayerUtil.noClipRender = true;
    }

    @Override
    protected void onDisable() {
        PlayerUtil.noClip = false;
        PlayerUtil.noClipRender = false;
    }
}

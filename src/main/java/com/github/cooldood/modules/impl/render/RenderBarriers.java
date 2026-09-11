package com.github.cooldood.modules.impl.render;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.ChatUtil;

@RegisterModule(
        name = "Render Barriers",
        description = "Provides Render Barriers functionality for the client.",
        category = Category.RENDER
)
public class RenderBarriers extends Module {

    @Override
    protected void onEnable() {
        C.mc.renderGlobal.loadRenderers();
        // actually too lazy to fact check this but i dont see why it wouldnt be true.
        ChatUtil.prefixMessage("To change the barrier texture create a texture pack and put your barrier texture in: &fassets/minecraft/textures/blocks/barrier.png");
    }

    @Override
    protected void onDisable() {
        C.mc.renderGlobal.loadRenderers();
    }
}

package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.modules.*;
import com.github.cooldood.utils.client.C;

@RegisterModule(
        name = "V-Clip",
        description = "Provides V-Clip functionality for the client.",
        category = Category.MOVEMENT
)
public class VClip extends Module {
    @RegisterSubModule(name = "V Clip Height", min = 1, max = 80, increment = 0.1)
    public double vClipHeight = 10;

    @RegisterSubModule(name = "Clip Up")
    public boolean clipUp = false;

    @Override
    protected void onEnable() {
        if (C.p() != null)
            C.p().setPosition(C.p().posX, C.p().posY + (clipUp ? vClipHeight : -vClipHeight), C.p().posZ);

        ModuleManager.getModule(VClip.class).toggle();
    }

    @Override
    protected void onDisable() {

    }
}

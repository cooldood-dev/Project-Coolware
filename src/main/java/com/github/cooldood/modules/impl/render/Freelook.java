package com.github.cooldood.modules.impl.render;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.PlayerUtil;
import com.github.cooldood.utils.minecraft.RotationUtil;

@RegisterModule(
        name = "Freelook",
        description = "Allows looking around without changing movement direction.",
        category = Category.RENDER
)
// todo: fix it setting rotation after its changed by server
public class Freelook extends Module {
    private int lastPerspective = 0;

    @Override
    protected void onEnable() {
        if (!C.isInGame() || ModuleManager.isEnabled(Freecam.class)) {
            this.toggle();
            return;
        }

        PlayerUtil.fakeRotation = PlayerUtil.realRotation = RotationUtil.getCurrentClientRotation();
        lastPerspective = C.mc.gameSettings.thirdPersonView;
        C.mc.gameSettings.thirdPersonView = 1;
    }

    @Override
    protected void onDisable() {
        if (!C.isInGame() || ModuleManager.isEnabled(Freecam.class)) return;

        if (C.mc.gameSettings.thirdPersonView == 1)
            C.mc.gameSettings.thirdPersonView = lastPerspective;

        // reset fake camera
        PlayerUtil.fakeRotation = null;
    }
}

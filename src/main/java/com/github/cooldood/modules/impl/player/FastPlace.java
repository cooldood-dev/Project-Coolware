package com.github.cooldood.modules.impl.player;

import com.github.cooldood.modules.*;
import com.github.cooldood.utils.minecraft.InventoryUtil;

@RegisterModule(
        name = "Fast Place",
        description = "Provides Fast Place functionality for the client.",
        category = Category.PLAYER
)
public class FastPlace extends Module {
    @RegisterSubModule(name = "Blocks Only")
    public static boolean blocksOnly = true;

    @RegisterSubModule(name = "Place Delay", min = 1, max = 5)
    public static int placeDelay = 1;

    public static int getPlaceDelay() {
        return ModuleManager.isEnabled(FastPlace.class) && (!blocksOnly || InventoryUtil.isValidBlock()) ? placeDelay : 4;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

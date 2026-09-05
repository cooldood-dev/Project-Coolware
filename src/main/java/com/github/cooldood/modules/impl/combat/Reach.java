package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.modules.*;

@RegisterModule(
        name = "Reach",
        description = "Extends your combat attack reach.",
        category = Category.COMBAT,
        dangerous = true
)
// who is even using reach in 2027
public class Reach extends Module {
    @RegisterSubModule(name = "Attack Reach", max = 6)
    public static double attackReach = 3;

    @RegisterSubModule(name = "Block Reach", max = 10)
    public static float blockReach = 4.5f;

    public static boolean shouldOverwriteReach() {
        return ModuleManager.isEnabled(Reach.class);
    }

    public static double getAttackReach() {
        return ModuleManager.isEnabled(Reach.class) ? attackReach : 6;
    }

    public static float getBlockReach() {
        return ModuleManager.isEnabled(Reach.class) ? blockReach : 6;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

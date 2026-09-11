package com.github.cooldood.modules.impl.movement;

import com.github.cooldood.modules.*;

@RegisterModule(
        name = "Keep Sprint",
        description = "Provides Keep Sprint functionality for the client.",
        category = Category.MOVEMENT,
        dangerous = true
)
public class KeepSprint extends Module {
    @RegisterSubModule(name = "Keep Sprint")
    public static boolean keepSprint = true;

    @RegisterSubModule(name = "Keep Motion")
    public static boolean keepMotion = true;

    public static boolean shouldKeepSprint() {
        return ModuleManager.isEnabled(KeepSprint.class) && keepSprint;
    }

    public static boolean shouldKeepMotion() {
        return ModuleManager.isEnabled(KeepSprint.class) && keepMotion;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}

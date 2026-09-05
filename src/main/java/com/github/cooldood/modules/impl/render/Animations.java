package com.github.cooldood.modules.impl.render;

import com.github.cooldood.modules.*;
import com.github.cooldood.utils.client.C;

@RegisterModule(
        name = "Animations",
        description = "Changes player swinging and blocking animations.",
        category = Category.RENDER
)
public class Animations extends Module {
    @RegisterSubModule(name = "Mode", description = "Which item animation to use in first person")
    public static AnimationMode mode = AnimationMode.VANILLA;

    @RegisterSubModule(name = "Render", description = "When to apply the custom animation")
    public static RenderMode render = RenderMode.ALWAYS;

    @RegisterSubModule(name = "Scale", min = 50, max = 150)
    public static int scale = 100;

    @RegisterSubModule(name = "Item Size", min = -0.5, max = 0.5, increment = 0.01)
    public static float itemSize = 0;

    @RegisterSubModule(name = "Block Pos X", min = -1, max = 1, increment = 0.01)
    public static float blockPosX = 0;

    @RegisterSubModule(name = "Block Pos Y", min = -1, max = 1, increment = 0.01)
    public static float blockPosY = 0;

    @RegisterSubModule(name = "Block Pos Z", min = -1, max = 1, increment = 0.01)
    public static float blockPosZ = 0;

    @RegisterSubModule(name = "Swing Speed", min = 0, max = 100)
    public static int swingSpeed = 0;

    public enum RenderMode {
        BLOCKING, ALWAYS
    }

    public enum AnimationMode {
        VANILLA, EXHIBITION, ETB, SIGMA, DORTWARE, PLAIN, SPIN, AVATAR, SWONG, SWANG, SWANK, STYLES,
        NUDGE, PUNCH, JIGSAW, SLIDE, SWING, OLD, PUSH, DASH, SLASH, SCALE, SWONK, STELLA, SMALL, EDIT,
        RHYS, STAB, FLOAT, REMIX, XIV, WINTER, YAMATO, SLIDE_SWING, SMALL_PUSH, REVERSE, INVENT, LEAKED,
        AQUA, ASTRO, FADEAWAY, ASTOLFO, ASTOLFO_SPIN, MOON, MOON_PUSH, SMOOTH, TAP1, TAP2, SIGMA3, SIGMA4,
        MYAU_1_8, MYAU_SLIDE, MYAU_SWANK, MYAU_SWANG, MYAU_AVATAR, MYAU_JIGSAW;
    }

    public static boolean shouldHideHeldItem() {
        return C.mc.gameSettings.thirdPersonView == 0
                && (ModuleManager.isEnabled(Freecam.class)
                || (ModuleManager.isEnabled(Zoom.class) && Zoom.hideHand)
                || ModuleManager.isEnabled(Freelook.class));
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    @Override
    public String arrayListExtraInfo() {
        return mode.name();
    }
}

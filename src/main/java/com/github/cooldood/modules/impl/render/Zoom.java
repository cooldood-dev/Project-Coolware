package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MouseScrolledEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import net.minecraft.util.MathHelper;

@RegisterModule(
        name = "Zoom",
        description = "Provides Zoom functionality for the client.",
        category = Category.RENDER
)
public class Zoom extends Module {
    @RegisterSubModule(name = "Zoom FOV", min = 1, max = 179)
    public static int zoomFOV = 30;

    @RegisterSubModule(name = "Scroll Zoom")
    public static boolean scrollZoom = true;

    @RegisterSubModule(name = "Scroll Zoom Indent", max = 20, parent = "Scroll Zoom")
    public static int scrollZoomIndent = 10;

    @RegisterSubModule(name = "Min Scroll Zoom", min = 1, max = 50, parent = "Scroll Zoom")
    public static int maxScrollZoom = 10;

    @RegisterSubModule(name = "Hide Hand")
    public static boolean hideHand = true;

    @RegisterSubModule(name = "Smooth Look")
    public static boolean smoothLook = true;

    private static float normalFOV = 0;
    private boolean wasSmoothLook = false;

    @SubscribeEvent
    public static void onMouseScrolledEvent(MouseScrolledEvent event) {
        if (scrollZoom) {
            event.setCancelled(true);

            C.mc.gameSettings.fovSetting = MathHelper.clamp_float(C.mc.gameSettings.fovSetting + (Math.signum(event.scrollAmount) * -scrollZoomIndent), maxScrollZoom, normalFOV);
        }
    }

    @Override
    protected void onEnable() {
        normalFOV = C.mc.gameSettings.fovSetting;
        wasSmoothLook = C.mc.gameSettings.smoothCamera;

        C.mc.gameSettings.fovSetting = zoomFOV;
        C.mc.gameSettings.smoothCamera |= smoothLook;
    }

    @Override
    protected void onDisable() {
        C.mc.gameSettings.fovSetting = normalFOV;
        C.mc.gameSettings.smoothCamera = wasSmoothLook;

        //C.mc.renderGlobal.loadRenderers();
    }
}

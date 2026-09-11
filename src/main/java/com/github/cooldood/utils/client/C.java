package com.github.cooldood.utils.client;

import com.github.cooldood.Main;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import org.reflections.Reflections;

// C for constant, C is short, not named C because i thought C.p() would be funny.
public class C {
    public static final Minecraft mc = Minecraft.getMinecraft();

    public static EntityPlayerSP p() {
        return mc.thePlayer;
    }

    public static WorldClient w() {
        return mc.theWorld;
    }

    public static boolean isInGame() {
        return p() != null && w() != null;
    }

    public final static Reflections reflections = new Reflections(Main.class.getPackage().getName());

    private static ScaledResolution cachedRes = null;

    /** Called once per frame by DraggableRenderer before any res() calls. */
    public static void updateResCache() {
        cachedRes = new ScaledResolution(mc);
    }

    public static ScaledResolution res() {
        return cachedRes != null ? cachedRes : new ScaledResolution(mc);
    }

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();
}
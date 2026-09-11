package com.github.cooldood.utils.minecraft;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.ClientTickEvent;
import com.github.cooldood.utils.client.C;
import lombok.Getter;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.Vec3;

import java.util.HashMap;

public class MovementUtil {
    public static int ticks = 0;
    public static int airTicks = 0;
    public static int groundTicks = 0;

    @SubscribeEvent(priority = 0)
    public static void onClientTickEvent(ClientTickEvent e) {
        if (C.isInGame()) {
            ticks++;
            airTicks++;
            groundTicks++;
            if (C.p().onGround) airTicks = 0;
            else groundTicks = 0;
        }
    }

    public static boolean isMoving(boolean y) {
        return C.p().posX != C.p().prevPosX || C.p().posZ != C.p().prevPosZ || (C.p().posY != C.p().prevPosY && y);
    }

    public static void setPos(Vec3 vec3) {
        C.p().setPosition(vec3.xCoord, vec3.yCoord, vec3.zCoord);
    }

    public static void setPrevPos(Vec3 vec3) {
        C.p().prevPosX = vec3.xCoord;
        C.p().prevPosY = vec3.yCoord;
        C.p().prevPosZ = vec3.zCoord;
    }

    @Getter
    private static final HashMap<KeyBinding, Boolean> overriddenKeybinds = new HashMap<>();

    public static void oneTickKeybind(KeyBinding keyBinding, boolean to) {
        overriddenKeybinds.put(keyBinding, to);
    }

    @SubscribeEvent(priority = 999)
    public static void onClientTick(ClientTickEvent event) {
        overriddenKeybinds.clear();
    }
}

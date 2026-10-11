package coolware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.Vec3;

import java.util.HashMap;

public class MovementUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static int ticks = 0;
    public static int airTicks = 0;
    public static int groundTicks = 0;

    public static void onClientTick() {
        if (mc.thePlayer != null && mc.theWorld != null) {
            ticks++;
            airTicks++;
            groundTicks++;
            if (mc.thePlayer.onGround) {
                airTicks = 0;
            } else {
                groundTicks = 0;
            }
        }
        overriddenKeybinds.clear();
    }

    public static boolean isMoving(boolean y) {
        if (mc.thePlayer == null) return false;
        return mc.thePlayer.posX != mc.thePlayer.prevPosX
                || mc.thePlayer.posZ != mc.thePlayer.prevPosZ
                || (mc.thePlayer.posY != mc.thePlayer.prevPosY && y);
    }

    public static void setPos(Vec3 vec3) {
        if (mc.thePlayer == null) return;
        mc.thePlayer.setPosition(vec3.xCoord, vec3.yCoord, vec3.zCoord);
    }

    public static void setPrevPos(Vec3 vec3) {
        if (mc.thePlayer == null) return;
        mc.thePlayer.prevPosX = vec3.xCoord;
        mc.thePlayer.prevPosY = vec3.yCoord;
        mc.thePlayer.prevPosZ = vec3.zCoord;
    }

    private static final HashMap<KeyBinding, Boolean> overriddenKeybinds = new HashMap<>();

    public static HashMap<KeyBinding, Boolean> getOverriddenKeybinds() {
        return overriddenKeybinds;
    }

    public static void oneTickKeybind(KeyBinding keyBinding, boolean to) {
        overriddenKeybinds.put(keyBinding, to);
    }
}

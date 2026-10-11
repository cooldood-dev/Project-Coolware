package coolware.config;

/**
 * Holds Animation configuration settings for first person rendering.
 */
public class AnimationConfig {
    public static AnimationMode mode       = AnimationMode.VANILLA;
    public static int           renderMode = 1; // 0 = BLOCKING, 1 = ALWAYS
    public static int           scale      = 100;
    public static float         itemSize   = 0.0F;
    public static float         blockPosX  = 0.0F;
    public static float         blockPosY  = 0.0F;
    public static float         blockPosZ  = 0.0F;
    public static int           swingSpeed = 6;
    public static boolean       enabled    = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        AnimationConfig.enabled = enabled;
    }

    public static AnimationMode getMode() {
        return mode;
    }

    public static void setMode(AnimationMode mode) {
        AnimationConfig.mode = mode;
    }

    public static int getRenderMode() {
        return renderMode;
    }

    public static void setRenderMode(int renderMode) {
        AnimationConfig.renderMode = renderMode;
    }

    public static int getScale() {
        return scale;
    }

    public static void setScale(int scale) {
        AnimationConfig.scale = scale;
    }

    public static float getItemSize() {
        return itemSize;
    }

    public static void setItemSize(float itemSize) {
        AnimationConfig.itemSize = itemSize;
    }

    public static float getBlockPosX() {
        return blockPosX;
    }

    public static void setBlockPosX(float blockPosX) {
        AnimationConfig.blockPosX = blockPosX;
    }

    public static float getBlockPosY() {
        return blockPosY;
    }

    public static void setBlockPosY(float blockPosY) {
        AnimationConfig.blockPosY = blockPosY;
    }

    public static float getBlockPosZ() {
        return blockPosZ;
    }

    public static void setBlockPosZ(float blockPosZ) {
        AnimationConfig.blockPosZ = blockPosZ;
    }

    public static int getSwingSpeed() {
        return swingSpeed;
    }

    public static void setSwingSpeed(int swingSpeed) {
        AnimationConfig.swingSpeed = swingSpeed;
    }
}

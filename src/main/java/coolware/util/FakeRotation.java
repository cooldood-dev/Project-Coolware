package coolware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

/**
 * What your own model shows, apart from the look you really have and the one
 * sent to the server: Scaffold's Fake Rotations. Set once a tick; the model
 * renderer (RendererLivingEntity.doRender) eases between the last two values
 * by the frame's partial tick. Only your player in the world -- never the
 * inventory's preview of you -- and only while active.
 */
public final class FakeRotation {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // The body is kept within this of the head, as vanilla turns the body.
    private static final float BODY_LAG = 50.0F;

    private static boolean active;
    private static float yaw, pitch;
    private static float prevYaw, prevPitch;

    private FakeRotation() {
    }

    /** This tick's look for the model. */
    public static void set(float newYaw, float newPitch) {
        if (active) {
            prevYaw = yaw;
            prevPitch = pitch;
        } else {
            prevYaw = newYaw;
            prevPitch = newPitch;
        }

        yaw = newYaw;
        pitch = MathHelper.clamp_float(newPitch, -90.0F, 90.0F);
        active = true;
    }

    /** Back to the look you really have. */
    public static void clear() {
        active = false;
    }

    public static boolean appliesTo(Entity entity) {
        return active && entity != null && mc.thePlayer != null && entity == mc.thePlayer
                && !(mc.currentScreen instanceof GuiInventory) && !(mc.currentScreen instanceof GuiContainerCreative);
    }

    public static float headYaw(float partialTicks) {
        return prevYaw + MathHelper.wrapAngleTo180_float(yaw - prevYaw) * partialTicks;
    }

    public static float pitch(float partialTicks) {
        return prevPitch + (pitch - prevPitch) * partialTicks;
    }

    /** The body's yaw: where vanilla has it, pulled along once the head is too far round. */
    public static float bodyYaw(float vanillaBody, float head) {
        float off = MathHelper.wrapAngleTo180_float(head - vanillaBody);

        if (off > BODY_LAG) return head - BODY_LAG;
        if (off < -BODY_LAG) return head + BODY_LAG;

        return vanillaBody;
    }
}

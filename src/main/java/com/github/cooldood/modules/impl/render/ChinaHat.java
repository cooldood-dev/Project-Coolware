package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RenderWorldEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.render.Render3dUtil;
import com.github.cooldood.utils.render.RenderUtil;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

@RegisterModule(
        name = "China Hat",
        description = "Renders a stylish cone hat above the player's head.",
        category = Category.RENDER
)
public class ChinaHat extends Module {

    @RegisterSubModule(name = "Radius", min = 0.3, max = 1.2, increment = 0.05)
    public static double radius = 0.65;

    @RegisterSubModule(name = "Height", min = 0.1, max = 0.6, increment = 0.05)
    public static double height = 0.28;

    @RegisterSubModule(name = "Sides", min = 12, max = 64, increment = 4)
    public static long sides = 32;

    @RegisterSubModule(name = "Only Third Person", description = "Only render hat when in third person view")
    public static boolean onlyThirdPerson = true;

    @RegisterSubModule(name = "Color Mode")
    public static HatColorMode colorMode = HatColorMode.Theme;
    public enum HatColorMode {
        Theme, Custom, Rainbow
    }

    @RegisterSubModule(name = "Custom Color", parent = "Color Mode")
    public static Color customColor = new Color(181, 166, 242);

    @RegisterSubModule(name = "Alpha", min = 20, max = 255, increment = 5)
    public static long alpha = 180;

    @RegisterSubModule(name = "Outline", description = "Render a crisp line around the bottom of the hat")
    public static boolean outline = true;

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldEvent event) {
        if (C.p() == null || C.mc.theWorld == null) return;
        if (onlyThirdPerson && C.mc.gameSettings.thirdPersonView == 0) return;

        EntityPlayerSP player = C.p();
        float partialTicks = event.partialTicks;

        // Interpolate player head position relative to camera
        Vec3 pos = Render3dUtil.getRelativeEntityPos(player, partialTicks);
        double x = pos.xCoord;
        double y = pos.yCoord + player.height + (player.isSneaking() ? -0.2 : 0.1);
        double z = pos.zCoord;

        // Head rotations
        float yaw = player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * partialTicks;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        // Rotate with player head
        GL11.glRotatef(-yaw, 0, 1, 0);
        GL11.glRotatef(pitch, 1, 0, 0);

        // Setup OpenGL state for cone rendering
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        int sideCount = (int) sides;
        int a = (int) Math.min(255, Math.max(0, alpha));

        // Draw cone surface (TRIANGLE_FAN)
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);

        // Tip of the hat (apex)
        Color tipColor = getHatColor(0);
        GL11.glColor4ub((byte) tipColor.getRed(), (byte) tipColor.getGreen(), (byte) tipColor.getBlue(), (byte) a);
        GL11.glVertex3d(0, height, 0);

        for (int i = 0; i <= sideCount; i++) {
            double angle = (i * Math.PI * 2) / sideCount;
            double vx = Math.sin(angle) * radius;
            double vz = Math.cos(angle) * radius;

            Color baseColor = getHatColor(i * (360.0 / sideCount));
            GL11.glColor4ub((byte) baseColor.getRed(), (byte) baseColor.getGreen(), (byte) baseColor.getBlue(), (byte) (a * 0.75f));
            GL11.glVertex3d(vx, 0, vz);
        }

        GL11.glEnd();

        // Optional bottom circle outline
        if (outline) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(2.0f);
            GL11.glBegin(GL11.GL_LINE_LOOP);

            for (int i = 0; i <= sideCount; i++) {
                double angle = (i * Math.PI * 2) / sideCount;
                double vx = Math.sin(angle) * radius;
                double vz = Math.cos(angle) * radius;

                Color lineColor = getHatColor(i * (360.0 / sideCount));
                GL11.glColor4ub((byte) lineColor.getRed(), (byte) lineColor.getGreen(), (byte) lineColor.getBlue(), (byte) 255);
                GL11.glVertex3d(vx, 0, vz);
            }

            GL11.glEnd();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }

        // Restore OpenGL state
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        GL11.glPopMatrix();
    }

    private static Color getHatColor(double angleOffset) {
        switch (colorMode) {
            case Rainbow:
                return RenderUtil.getColorsFade(angleOffset, ThemeModule.getThemeColours(), 2.0);
            case Custom:
                return customColor;
            case Theme:
            default:
                return ThemeModule.getAccentColor(angleOffset);
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}
}

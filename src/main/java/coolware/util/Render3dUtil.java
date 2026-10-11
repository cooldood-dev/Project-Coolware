package coolware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class Render3dUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void add3DVertex(double x, double y, double z) {
        Tessellator.getInstance().getWorldRenderer().pos(x, y, z).endVertex();
    }

    public static void add3DVertexColor(float x, float y, Color color) {
        Tessellator.getInstance().getWorldRenderer().pos(x, y, 0)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
    }

    public static void add3DVertexColor(double x, double y, double z, Color color) {
        Tessellator.getInstance().getWorldRenderer().pos(x, y, z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
    }

    public static void drawCentered3dBox(double x, double y, double z, double w, double h, double d, Color color, float partialTicks, boolean cull) {
        draw3dBox(x - w / 2, y - h / 2, z - d / 2, w, h, d, color, partialTicks, cull);
    }

    public static void drawCentered3dBox(double x, double y, double z, double w, double h, double d, Color color, float partialTicks) {
        draw3dBox(x - w / 2, y - h / 2, z - d / 2, w, h, d, color, partialTicks, false);
    }

    public static void draw3dBox(double x, double y, double z, double w, double h, double d, Color color, float partialTicks) {
        draw3dBox(x, y, z, w, h, d, color, partialTicks, false);
    }

    public static void draw3dBox(double x, double y, double z, double w, double h, double d, Color color, float partialTicks, boolean cull) {
        draw3dBox(x, y, z, w, h, d, color, partialTicks, true, true, true, true, true, true, cull);
    }

    public static void draw3dBox(double x, double y, double z, double w, double h, double d, Color color, float partialTicks,
                                 boolean down, boolean up, boolean north, boolean south, boolean west, boolean east, boolean cull) {
        Vec3 relativeCoordinatePos = getRelativePos(new Vec3(x, y, z), partialTicks);
        x = relativeCoordinatePos.xCoord;
        y = relativeCoordinatePos.yCoord;
        z = relativeCoordinatePos.zCoord;

        GL11.glPushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableAlpha();
        if (!cull) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        GlStateManager.disableTexture2D();

        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);

        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        if (down) {
            wr.pos(x, y, z).endVertex();
            wr.pos(x + w, y, z).endVertex();
            wr.pos(x + w, y, z + d).endVertex();
            wr.pos(x, y, z + d).endVertex();
        }

        if (up) {
            wr.pos(x, y + h, z + d).endVertex();
            wr.pos(x + w, y + h, z + d).endVertex();
            wr.pos(x + w, y + h, z).endVertex();
            wr.pos(x, y + h, z).endVertex();
        }

        if (west) {
            wr.pos(x, y, z + d).endVertex();
            wr.pos(x, y + h, z + d).endVertex();
            wr.pos(x, y + h, z).endVertex();
            wr.pos(x, y, z).endVertex();
        }

        if (east) {
            wr.pos(x + w, y, z).endVertex();
            wr.pos(x + w, y + h, z).endVertex();
            wr.pos(x + w, y + h, z + d).endVertex();
            wr.pos(x + w, y, z + d).endVertex();
        }

        if (north) {
            wr.pos(x, y + h, z).endVertex();
            wr.pos(x + w, y + h, z).endVertex();
            wr.pos(x + w, y, z).endVertex();
            wr.pos(x, y, z).endVertex();
        }

        if (south) {
            wr.pos(x, y, z + d).endVertex();
            wr.pos(x + w, y, z + d).endVertex();
            wr.pos(x + w, y + h, z + d).endVertex();
            wr.pos(x, y + h, z + d).endVertex();
        }

        Tessellator.getInstance().draw();

        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GlStateManager.enableTexture2D();
        GL11.glPopMatrix();
    }

    public static Vec3 getRelativeEntityPos(Entity entity, float partialTicks) {
        if (entity == null) return new Vec3(0, 0, 0);
        Vec3 entityLerped = lerpVec(new Vec3(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ), entity.getPositionVector(), partialTicks);
        return getRelativePos(entityLerped, partialTicks);
    }

    public static Vec3 getRelativePos(Vec3 vec3, float partialTicks) {
        if (mc.thePlayer == null) return vec3;
        Vec3 playerLerped = lerpVec(new Vec3(mc.thePlayer.lastTickPosX, mc.thePlayer.lastTickPosY, mc.thePlayer.lastTickPosZ), mc.thePlayer.getPositionVector(), partialTicks);
        return new Vec3(
                vec3.xCoord - playerLerped.xCoord,
                vec3.yCoord - playerLerped.yCoord,
                vec3.zCoord - playerLerped.zCoord
        );
    }

    public static Vec3 getRelativePos(BlockPos blockPos, float partialTicks) {
        if (mc.thePlayer == null) return new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        Vec3 playerLerped = lerpVec(new Vec3(mc.thePlayer.lastTickPosX, mc.thePlayer.lastTickPosY, mc.thePlayer.lastTickPosZ), mc.thePlayer.getPositionVector(), partialTicks);
        return new Vec3(
                blockPos.getX() - playerLerped.xCoord,
                blockPos.getY() - playerLerped.yCoord,
                blockPos.getZ() - playerLerped.zCoord
        );
    }

    public static Vec3 lerpVec(Vec3 prevVec, Vec3 currentVec, float partialTicks) {
        return new Vec3(
                prevVec.xCoord + (currentVec.xCoord - prevVec.xCoord) * partialTicks,
                prevVec.yCoord + (currentVec.yCoord - prevVec.yCoord) * partialTicks,
                prevVec.zCoord + (currentVec.zCoord - prevVec.zCoord) * partialTicks
        );
    }
}

package com.github.cooldood.utils.render;

import com.github.cooldood.utils.client.C;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class Render3dUtil {

    public static void add3DVertex(double x, double y, double z) {
        RenderUtil.worldRenderer.pos(x, y, z).endVertex();
    }

    public static void add3DVertexColor(float x, float y, Color color) {
        RenderUtil.worldRenderer.pos(x, y, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
    }

    public static void add3DVertexColor(double x, double y, double z, Color color) {
        RenderUtil.worldRenderer.pos(x, y, z).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
    }


    public static void drawCentered3dBox(double x, double y, double z, double w, double h, double d, Color color, float partialTicks, boolean cull) {
        draw3dBox(x-w/2, y-h/2, z-d/2, w, h, d, color, partialTicks, cull);
    }

    public static void drawCentered3dBox(double x, double y, double z, double w, double h, double d, Color color, float partialTicks) {
        draw3dBox(x-w/2, y-h/2, z-d/2, w, h, d, color, partialTicks, false);
    }

    public static void draw3dBox(double x, double y, double z, double w, double h, double d, Color color, float partialTicks) {
        draw3dBox(x, y, z, w, h, d, color, partialTicks, false);
    }

    public static void draw3dBox(double x, double y, double z, double w, double h, double d, Color color, float partialTicks, boolean cull) {
        draw3dBox(x, y, z, w, h, d, color, partialTicks, true, true, true, true, true, true, cull);
    }

    public static void draw3dBox(double x, double y, double z, double w, double h, double d, Color color, float partialTicks, boolean down, boolean up, boolean north, boolean south, boolean west, boolean east, boolean cull) {
        // setup drawing
        Vec3 relativeCoordinatePos = getRelativePos(new Vec3(x, y, z), partialTicks);
        x = relativeCoordinatePos.xCoord; y = relativeCoordinatePos.yCoord; z = relativeCoordinatePos.zCoord;

        RenderUtil.beginRender();
        if (cull) GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderUtil.glColor(color);
        RenderUtil.beginAddingVertex(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        if (down) {
            add3DVertex(x, y, z);
            add3DVertex(x + w, y, z);
            add3DVertex(x + w, y, z + d);
            add3DVertex(x, y, z + d);
        }

        if (up) {
            add3DVertex(x, y + h, z + d);
            add3DVertex(x + w, y + h, z + d);
            add3DVertex(x + w, y + h, z);
            add3DVertex(x, y + h, z);
        }

        if (west) {
            add3DVertex(x, y, z + d);
            add3DVertex(x, y + h, z + d);
            add3DVertex(x, y + h, z);
            add3DVertex(x, y, z);
        }

        if (east) {
            add3DVertex(x + w, y, z);
            add3DVertex(x + w, y + h, z);
            add3DVertex(x + w, y + h, z + d);
            add3DVertex(x + w, y, z + d);
        }

        if (north) {
            add3DVertex(x, y + h, z);
            add3DVertex(x + w, y + h, z);
            add3DVertex(x + w, y, z);
            add3DVertex(x, y, z);
        }

        if (south) {
            add3DVertex(x, y, z + d);
            add3DVertex(x + w, y, z + d);
            add3DVertex(x + w, y + h, z + d);
            add3DVertex(x, y + h, z + d);
        }


        // we have finished! draw!
        RenderUtil.finishRender();
    }

    public static void drawRoundedRectGlow(float x, float y, float w, float h, float radius, Color color) {
        // makes sure the rectangle doesn't bend in on itself
        radius = Math.min(radius, w / 2);
        radius = Math.min(radius, h / 2);

        // setup drawing
        RenderUtil.beginRender();
        RenderUtil.beginAddingVertex(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        add3DVertexColor(
                x + (w/2),
                y + (h/2),
                color
        );

        for (int i = 0; i <= 360; i+=10) {
            float xCornerOffset = (i > 180 ? w-(radius*2) : 0);
            float yCornerOffset = (i > 90 && i <= 270 ? h-(radius*2) : 0);

            add3DVertexColor(
                    (float) (x + radius + (Math.sin((i * Math.PI) / 180)) * (radius * -1)) + xCornerOffset,
                    (float) (y + radius + (Math.cos((i * Math.PI) / 180)) * (radius * -1)) + yCornerOffset,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
            );
        }


        add3DVertexColor(
                x + radius,
                y,
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
        );

        // we have finished! draw!
        RenderUtil.finishRender();
    }


    public static void rotateToPlayer(boolean pitch) {
        GL11.glRotatef(-C.p().rotationYaw, 0,1,0);
        if (C.mc.gameSettings.thirdPersonView == 2) {
            GL11.glRotatef(180, 0, 1, 0);
            if (pitch) GL11.glRotatef(-C.p().rotationPitch, 1,0,0);
        }
        else if (pitch) GL11.glRotatef(C.p().rotationPitch, 1,0,0);
    }

    public static Vec3 getRelativeEntityPos(Entity entity, float partialTicks) {
        Vec3 entityLerped = lerpVec(new Vec3(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ), entity.getPositionVector(), partialTicks);
        return getRelativePos(entityLerped, partialTicks);
    }

    public static Vec3 getRelativePos(Vec3 vec3, float partialTicks) {
        Vec3 playerLerped = lerpVec(new Vec3(C.p().lastTickPosX, C.p().lastTickPosY, C.p().lastTickPosZ), C.p().getPositionVector(), partialTicks);
        return new Vec3(
                vec3.xCoord - playerLerped.xCoord,
                vec3.yCoord - playerLerped.yCoord,
                vec3.zCoord - playerLerped.zCoord
        );
    }

    public static Vec3 getRelativePos(BlockPos blockPos, float partialTicks) {
        Vec3 playerLerped = lerpVec(new Vec3(C.p().lastTickPosX, C.p().lastTickPosY, C.p().lastTickPosZ), C.p().getPositionVector(), partialTicks);
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

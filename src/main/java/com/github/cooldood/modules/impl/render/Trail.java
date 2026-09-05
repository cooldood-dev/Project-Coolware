package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.events.impl.RenderWorldEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.MovementUtil;
import com.github.cooldood.utils.render.Render3dUtil;
import com.github.cooldood.utils.render.RenderUtil;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;

@RegisterModule(
        name = "Trail",
        description = "Provides Trail functionality for the client.",
        category = Category.RENDER
)
public class Trail extends Module {
    @RegisterSubModule(name = "Mode")
    public static Mode mode = Mode.Dots;
    public enum Mode {
        Dots,
        Line
    }

    @RegisterSubModule(name = "Line Width", parent = "Mode", modeParentString = "Line", min = 1, max = 5)
    public static int lineWidth = 2;

    @RegisterSubModule(name = "Trail Length", min = 5, max = 500)
    public static int trailLength = 10;

    @RegisterSubModule(name = "Fade Speed", max = 10, increment = 0.1)
    public static float fadeSpeed = 2;

    @RegisterSubModule(name = "Draw On Ground")
    public static boolean drawOnGround = false;

    private static final ArrayList<PosAndTime> nodes = new ArrayList<>();

    @SubscribeEvent
    public static void onPlayerUpdate(PlayerUpdateEvent event) {
        if (MovementUtil.isMoving(false)) {
            Vec3 pos = C.p().getPositionVector();
            if (drawOnGround) {
                Vec3 voidPos = new Vec3(C.p().posX, 0, C.p().posZ);
                MovingObjectPosition raytrace = C.w().rayTraceBlocks(C.p().getPositionVector(), voidPos, true, true, false);
                if (raytrace != null && raytrace.hitVec != null && C.p().posY > 0)
                    pos = raytrace.hitVec;
            }

            nodes.add(new PosAndTime(pos, System.currentTimeMillis()));
        }

        while (nodes.size() > trailLength) nodes.remove(0);
    }

    @SubscribeEvent
    public static void onRender3d(RenderWorldEvent event) {
        if (mode == Mode.Line) {
            RenderUtil.beginRender();
            RenderUtil.beginAddingVertex(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            GL11.glLineWidth(lineWidth);
            for (PosAndTime node : nodes) {
                Color color = RenderUtil.getColorsFade(node.time, ThemeModule.getThemeColours(), fadeSpeed);
                Vec3 relativeCoordinatePos = Render3dUtil.getRelativePos(node.pos, event.partialTicks);
                RenderUtil.getTessalator().getWorldRenderer()
                        .pos(relativeCoordinatePos.xCoord, relativeCoordinatePos.yCoord, relativeCoordinatePos.zCoord)
                        .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                        .endVertex();
            }
            RenderUtil.finishRender();
        }
        else {
            for (PosAndTime node : nodes) {
                Color color = RenderUtil.getColorsFade(node.time, ThemeModule.getThemeColours(), fadeSpeed);

                GL11.glPushMatrix();
                RenderUtil.glTranslate(Render3dUtil.getRelativePos(node.pos, event.partialTicks));
                Render3dUtil.rotateToPlayer(true);
                Render3dUtil.drawRoundedRectGlow(-0.1f, -0.1f, 0.2f, 0.2f, 0.2f, color);
                GL11.glPopMatrix();
            }
        }
    }

    @AllArgsConstructor
    private static class PosAndTime {
        public Vec3 pos;
        public long time;
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}
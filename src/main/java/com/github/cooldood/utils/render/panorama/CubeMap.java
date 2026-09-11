package com.github.cooldood.utils.render.panorama;

import com.github.cooldood.utils.client.C;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

public class CubeMap {
    private final ResourceLocation[] images = new ResourceLocation[6];

    public CubeMap(ResourceLocation baseImage) {
        for (int x = 0; x < 6; ++x) {
            this.images[x] = new ResourceLocation(baseImage.getResourceDomain(), String.format("%s_%d.png",
                    baseImage.getResourcePath(), x));
        }
    }

    public void render(Minecraft mc, float float2, float float3, float opacityMultiplier) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(85f, (float)mc.displayWidth / (float)mc.displayHeight,
                0.05f, 10f);
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.rotate(180f, 1f, 0f, 0f);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        for (int pass = 0; pass < 4; ++pass) {
            GlStateManager.pushMatrix();
            float x = ((float)(pass % 2) / 2f - 0.5f) / 256f;
            float y = ((float)(pass / 2) / 2f - 0.5f) / 256f;
            GlStateManager.translate(x, y, 0f);
            GlStateManager.rotate(float2, 1f, 0f, 0f);
            GlStateManager.rotate(float3, 0f, 1f, 0f);
            for (int renderedImage = 0; renderedImage < 6; ++renderedImage) {
                mc.getTextureManager().bindTexture(this.images[renderedImage]);
                wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                int opacity = Math.round(255f * opacityMultiplier) / (pass + 1);
                if (renderedImage == 0) {
                    wr.pos(-1.0, -1.0, 1.0).tex(0.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(-1.0, 1.0, 1.0).tex(0.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(1.0, 1.0, 1.0).tex(1.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(1.0, -1.0, 1.0).tex(1.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                }
                if (renderedImage == 1) {
                    wr.pos(1.0, -1.0, 1.0).tex(0.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(1.0, 1.0, 1.0).tex(0.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(1.0, 1.0, -1.0).tex(1.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(1.0, -1.0, -1.0).tex(1.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                }
                if (renderedImage == 2) {
                    wr.pos(1.0, -1.0, -1.0).tex(0.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(1.0, 1.0, -1.0).tex(0.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(-1.0, 1.0, -1.0).tex(1.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(-1.0, -1.0, -1.0).tex(1.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                }
                if (renderedImage == 3) {
                    wr.pos(-1.0, -1.0, -1.0).tex(0.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(-1.0, 1.0, -1.0).tex(0.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(-1.0, 1.0, 1.0).tex(1.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(-1.0, -1.0, 1.0).tex(1.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                }
                if (renderedImage == 4) {
                    wr.pos(-1.0, -1.0, -1.0).tex(0.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(-1.0, -1.0, 1.0).tex(0.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(1.0, -1.0, 1.0).tex(1.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(1.0, -1.0, -1.0).tex(1.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                }
                if (renderedImage == 5) {
                    wr.pos(-1.0, 1.0, 1.0).tex(0.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(-1.0, 1.0, -1.0).tex(0.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(1.0, 1.0, -1.0).tex(1.0, 1.0)
                            .color(255, 255, 255, opacity).endVertex();
                    wr.pos(1.0, 1.0, 1.0).tex(1.0, 0.0)
                            .color(255, 255, 255, opacity).endVertex();
                }
                tessellator.draw();
            }
            GlStateManager.popMatrix();
            GlStateManager.colorMask(true, true, true, false);
        }
        wr.setTranslation(0.0, 0.0, 0.0);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.matrixMode(5889);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
    }


    public void setupGuiState(boolean macOS) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = C.res();

        GlStateManager.clear(256);
        if (macOS) {
            GL11.glGetError();
        }
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0,
                (double)mc.displayWidth / sr.getScaleFactor(),
                (double)mc.displayHeight / sr.getScaleFactor(),
                0.0, 1000.0, 3000.0);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0f, 0f, -2000f);
    }
}
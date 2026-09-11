package com.github.cooldood.utils.tenacity.render.blur;
import com.github.cooldood.utils.render.RenderUtil;

import net.minecraft.client.Minecraft;


import com.github.cooldood.utils.tenacity.render.ShaderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_LINEAR;

public class KawaseBlur  {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static ShaderUtil kawaseDown = new ShaderUtil("kawaseDown");
    public static ShaderUtil kawaseUp = new ShaderUtil("kawaseUp");

    public static Framebuffer framebuffer = new Framebuffer(1, 1, false);

    public static void setupUniforms(float offset) {
        kawaseDown.setUniformf("offset", offset, offset);
        kawaseUp.setUniformf("offset", offset, offset);
    }

    private static int currentIterations;

    @com.github.cooldood.events.SubscribeEvent
    public static void onWindowResize(com.github.cooldood.events.impl.WindowResizeEvent event) {
        currentIterations = -1;
    }

    private static final List<Framebuffer> framebufferList = new ArrayList<>();

    private static void initFramebuffers(float iterations) {
        try {
            for (Framebuffer framebuffer : framebufferList) {
                try {
                    framebuffer.deleteFramebuffer();
                } catch (Throwable ignored) {
                    // Continue releasing the remaining framebuffers even if one fails;
                    // skipping the loop would leak GPU memory permanently.
                }
            }
        } finally {
            framebufferList.clear();
        }

        //Have to make the framebuffer null so that it does not try to delete a framebuffer that has already been deleted
        framebufferList.add(framebuffer = RenderUtil.createFrameBuffer(null));


        for (int i = 1; i <= iterations; i++) {
            Framebuffer currentBuffer = new Framebuffer((int) (mc.displayWidth / Math.pow(2, i)), (int) (mc.displayHeight / Math.pow(2, i)), false);
            currentBuffer.setFramebufferFilter(GL_LINEAR);
            GlStateManager.bindTexture(currentBuffer.framebufferTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL14.GL_MIRRORED_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL14.GL_MIRRORED_REPEAT);
            GlStateManager.bindTexture(0);

            framebufferList.add(currentBuffer);
        }
    }


    public static void renderBlur(int stencilFrameBufferTexture, int iterations, int offset) {
        if (currentIterations != iterations || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
            initFramebuffers(iterations);
            currentIterations = iterations;
        }

        renderFBO(framebufferList.get(1), mc.getFramebuffer().framebufferTexture, kawaseDown, offset);

        //Downsample
        for (int i = 1; i < iterations; i++) {
            renderFBO(framebufferList.get(i + 1), framebufferList.get(i).framebufferTexture, kawaseDown, offset);
        }

        //Upsample
        for (int i = iterations; i > 1; i--) {
            renderFBO(framebufferList.get(i - 1), framebufferList.get(i).framebufferTexture, kawaseUp, offset);
        }


        Framebuffer lastBuffer = framebufferList.get(0);
        lastBuffer.framebufferClear();
        lastBuffer.bindFramebuffer(false);
        kawaseUp.init();
        try {
            kawaseUp.setUniformf("offset", offset, offset);
            kawaseUp.setUniformi("inTexture", 0);
            kawaseUp.setUniformi("check", 1);
            kawaseUp.setUniformi("textureToCheck", 16);
            kawaseUp.setUniformf("halfpixel", 1.0f / lastBuffer.framebufferWidth, 1.0f / lastBuffer.framebufferHeight);
            kawaseUp.setUniformf("iResolution", lastBuffer.framebufferWidth, lastBuffer.framebufferHeight);
            GL13.glActiveTexture(GL13.GL_TEXTURE16);
            RenderUtil.bindTexture(stencilFrameBufferTexture);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            RenderUtil.bindTexture(framebufferList.get(1).framebufferTexture);
            ShaderUtil.drawQuads(mc.displayWidth, mc.displayHeight);
        } finally {
            kawaseUp.unload();
            GL13.glActiveTexture(GL13.GL_TEXTURE16);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        }


        mc.getFramebuffer().bindFramebuffer(true);
        RenderUtil.bindTexture(framebufferList.get(0).framebufferTexture);
        RenderUtil.setAlphaLimit(0);
        GlStateManager.enableBlend(); GlStateManager.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        ShaderUtil.drawQuads();
        GlStateManager.bindTexture(0);

    }

    private static void renderFBO(Framebuffer framebuffer, int framebufferTexture, ShaderUtil shader, float offset) {
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(false);
        shader.init();
        try {
            RenderUtil.bindTexture(framebufferTexture);
            shader.setUniformf("offset", offset, offset);
            shader.setUniformi("inTexture", 0);
            shader.setUniformi("check", 0);
            shader.setUniformf("halfpixel", 1.0f / framebuffer.framebufferWidth, 1.0f / framebuffer.framebufferHeight);
            shader.setUniformf("iResolution", framebuffer.framebufferWidth, framebuffer.framebufferHeight);
            ShaderUtil.drawQuads(framebuffer.framebufferWidth, framebuffer.framebufferHeight);
        } finally {
            shader.unload();
        }
    }


}

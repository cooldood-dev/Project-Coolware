package coolware.util.shader.impl;

import coolware.util.shader.base.RiseShader;
import coolware.util.shader.base.RiseShaderProgram;
import coolware.util.shader.base.ShaderRenderType;
import coolware.util.shader.base.ShaderUniforms;
import coolware.util.shader.kernel.GaussianKernel;
import java.nio.FloatBuffer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class BloomShader extends RiseShader {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final RiseShaderProgram bloomProgram = new RiseShaderProgram("bloom.frag", "vertex.vsh");
    private Framebuffer inputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
    private Framebuffer outputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
    private GaussianKernel gaussianKernel = new GaussianKernel(0);

    public BloomShader() {
    }

    @Override
    public void render(ShaderRenderType renderType, float partialTicks, List<Runnable> runnables) {
        if (Display.isVisible()) {
            boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
            switch (renderType) {
                case CAMERA:
                    RendererLivingEntity.NAME_TAG_RANGE = 0.0F;
                    RendererLivingEntity.NAME_TAG_RANGE_SNEAK = 0.0F;
                    this.inputFramebuffer.bindFramebuffer(true);

                    for (Runnable runnable : runnables) {
                        runnable.run();
                    }

                    mc.getFramebuffer().bindFramebuffer(true);
                    RendererLivingEntity.NAME_TAG_RANGE = 64.0F;
                    RendererLivingEntity.NAME_TAG_RANGE_SNEAK = 32.0F;
                    RenderHelper.disableStandardItemLighting();
                    mc.entityRenderer.disableLightmap();
                    break;

                case OVERLAY:
                    this.inputFramebuffer.bindFramebuffer(true);

                    for (Runnable runnable1 : runnables) {
                        runnable1.run();
                    }

                    int radius = 14;
                    float compression = 2.0F;
                    int programId = this.bloomProgram.getProgramId();
                    this.outputFramebuffer.bindFramebuffer(true);
                    this.bloomProgram.start();

                    if (this.gaussianKernel.getSize() != radius) {
                        this.gaussianKernel = new GaussianKernel(radius);
                        this.gaussianKernel.compute();
                        FloatBuffer floatbuffer = BufferUtils.createFloatBuffer(radius);
                        floatbuffer.put(this.gaussianKernel.getKernel());
                        floatbuffer.flip();
                        ShaderUniforms.uniform1f(programId, "u_radius", (float)radius);
                        ShaderUniforms.uniformFB(programId, "u_kernel", floatbuffer);
                        ShaderUniforms.uniform1i(programId, "u_diffuse_sampler", 0);
                        ShaderUniforms.uniform1i(programId, "u_other_sampler", isMac ? 3 : 16);
                    }

                    ShaderUniforms.uniform2f(programId, "u_texel_size", 1.0F / mc.displayWidth, 1.0F / mc.displayHeight);
                    ShaderUniforms.uniform2f(programId, "u_direction", compression, 0.0F);
                    GlStateManager.enableBlend();
                    GlStateManager.blendFunc(1, 770);
                    GlStateManager.alphaFunc(516, 0.0F);
                    this.inputFramebuffer.bindFramebufferTexture();
                    RiseShaderProgram.drawScaledQuad();

                    mc.getFramebuffer().bindFramebuffer(true);
                    GlStateManager.blendFunc(770, 771);
                    ShaderUniforms.uniform2f(programId, "u_direction", 0.0F, compression);
                    GL13.glActiveTexture(isMac ? 33987 : 34000);
                    this.inputFramebuffer.bindFramebufferTexture();
                    GL13.glActiveTexture(33984);
                    RiseShaderProgram.drawScaledQuad();
                    GlStateManager.disableBlend();
                    RiseShaderProgram.stop();

                    // Cleanly unbind textures and reset active texture unit
                    GL13.glActiveTexture(isMac ? 33987 : 34000);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                    GL13.glActiveTexture(33984);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                    GlStateManager.enableTexture2D();
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    break;
            }
        }
    }

    @Override
    public void update() {
        int width = mc.displayWidth;
        int height = mc.displayHeight;
        if (this.inputFramebuffer.framebufferWidth != width || this.inputFramebuffer.framebufferHeight != height) {
            this.inputFramebuffer.deleteFramebuffer();
            this.inputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
        } else {
            this.inputFramebuffer.framebufferClear();
        }

        if (this.outputFramebuffer.framebufferWidth != width || this.outputFramebuffer.framebufferHeight != height) {
            this.outputFramebuffer.deleteFramebuffer();
            this.outputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
        } else {
            this.outputFramebuffer.framebufferClear();
        }

        this.inputFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        this.outputFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
    }
}

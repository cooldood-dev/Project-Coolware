package coolware.gui.hud;

import coolware.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class IslandBlur {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int DOWNSAMPLE = 2; // 2x downsampling for 4x performance boost & softer glass blur
    private static final int BLUR_RADIUS = 16;
    private static final int BLUR_SAMPLE_PADDING = 16;
    private static final IntBuffer SCISSOR_BUFFER = BufferUtils.createIntBuffer(4);
    private static final FloatBuffer COLOR_BUFFER = BufferUtils.createFloatBuffer(4);
    private static Framebuffer horizontalBuffer;
    private static Framebuffer blurBuffer;
    private static int program;
    private static int roundedProgram;
    private static int textureUniform;
    private static int texelSizeUniform;
    private static int directionUniform;
    private static int radiusUniform;
    private static int roundedTextureUniform;
    private static int panelMinUniform;
    private static int panelSizeUniform;
    private static int cornerRadiusUniform;
    private static int lastDisplayWidth = -1;
    private static int lastDisplayHeight = -1;

    private IslandBlur() {}

    public static boolean isSupported() {
        return OpenGlHelper.areShadersSupported() && OpenGlHelper.isFramebufferEnabled();
    }

    public static void renderBlur(float x, float y, float width, float height, float radius) {
        renderBlur(x, y, width, height, radius, -1.0F);
    }

    public static void renderBlurRounded(float x, float y, float width, float height,
                                         float blurRadius, float cornerRadius) {
        renderBlur(x, y, width, height, blurRadius, cornerRadius);
    }

    private static void renderBlur(float x, float y, float width, float height, float radius,
                                   float cornerRadius) {
        if (!isSupported() || mc.getFramebuffer() == null || width <= 0 || height <= 0) {
            return;
        }

        try {
            ensureResources();
            if (horizontalBuffer == null || blurBuffer == null || program == 0
                    || (cornerRadius >= 0.0F && roundedProgram == 0)) {
                return;
            }

            int displayWidth = mc.displayWidth;
            int displayHeight = mc.displayHeight;
            int targetW = Math.max(1, displayWidth / DOWNSAMPLE);
            int targetH = Math.max(1, displayHeight / DOWNSAMPLE);

            ScaledResolution sr = new ScaledResolution(mc);
            int screenWidth = sr.getScaledWidth();
            int screenHeight = sr.getScaledHeight();

            boolean wasBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            boolean wasDepthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean wasAlphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            boolean wasTextureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            boolean wasCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            boolean wasScissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            int boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            int alphaFunction = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
            float alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
            SCISSOR_BUFFER.clear();
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, SCISSOR_BUFFER);
            int scissorX = SCISSOR_BUFFER.get(0);
            int scissorY = SCISSOR_BUFFER.get(1);
            int scissorWidth = SCISSOR_BUFFER.get(2);
            int scissorHeight = SCISSOR_BUFFER.get(3);
            COLOR_BUFFER.clear();
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, COLOR_BUFFER);
            float red = COLOR_BUFFER.get(0);
            float green = COLOR_BUFFER.get(1);
            float blue = COLOR_BUFFER.get(2);
            float alpha = COLOR_BUFFER.get(3);
            int blendSrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
            int blendDst = GL11.glGetInteger(GL11.GL_BLEND_DST);
            int blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            int blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            float passRadius = cornerRadius >= 0.0F
                    ? Math.max(1.0F, Math.min(BLUR_RADIUS, radius))
                    : BLUR_RADIUS;

            try {
                GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
                GlStateManager.disableDepth();
                GlStateManager.disableAlpha();
                GlStateManager.enableBlend();
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        GL11.GL_ONE, GL11.GL_ZERO);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                // Pass 1: Downsampled Horizontal blur
                renderBlurPass(mc.getFramebuffer().framebufferTexture, horizontalBuffer, targetW, targetH,
                        screenWidth, screenHeight, 1.0F, 0.0F, passRadius,
                        cornerRadius >= 0.0F, x, y, width, height, BLUR_SAMPLE_PADDING);

                // Pass 2: Downsampled Vertical blur
                renderBlurPass(horizontalBuffer.framebufferTexture, blurBuffer, targetW, targetH,
                        screenWidth, screenHeight, 0.0F, 1.0F, passRadius,
                        cornerRadius >= 0.0F, x, y, width, height, 1);

                // Restore MC framebuffer
                mc.getFramebuffer().bindFramebuffer(true);
                GlStateManager.viewport(0, 0, displayWidth, displayHeight);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);

                GlStateManager.enableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.bindTexture(blurBuffer.framebufferTexture);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                if (cornerRadius >= 0.0F) {
                    float scale = sr.getScaleFactor();
                    GL20.glUseProgram(roundedProgram);
                    GL20.glUniform1i(roundedTextureUniform, 0);
                    GL20.glUniform2f(panelMinUniform, x * scale,
                            displayHeight - (y + height) * scale);
                    GL20.glUniform2f(panelSizeUniform, width * scale, height * scale);
                    GL20.glUniform1f(cornerRadiusUniform, cornerRadius * scale);
                    RenderUtil.prepareScissor((int) Math.floor(x), (int) Math.floor(y),
                            (int) Math.ceil(width), (int) Math.ceil(height));
                    drawQuad(screenWidth, screenHeight);
                    RenderUtil.endScissor();
                } else {
                    RenderUtil.prepareScissor((int) Math.floor(x), (int) Math.floor(y),
                            (int) Math.ceil(width), (int) Math.ceil(height));
                    drawQuad(screenWidth, screenHeight);
                    RenderUtil.endScissor();
                }

            } finally {
                GL20.glUseProgram(currentProgram);
                mc.getFramebuffer().bindFramebuffer(true);
                GlStateManager.viewport(0, 0, displayWidth, displayHeight);
                GlStateManager.tryBlendFuncSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha);
                restoreCapability(GL11.GL_BLEND, wasBlendEnabled);
                restoreCapability(GL11.GL_DEPTH_TEST, wasDepthEnabled);
                restoreCapability(GL11.GL_ALPHA_TEST, wasAlphaEnabled);
                restoreCapability(GL11.GL_TEXTURE_2D, wasTextureEnabled);
                restoreCapability(GL11.GL_CULL_FACE, wasCullEnabled);
                GlStateManager.alphaFunc(alphaFunction, alphaReference);
                if (wasScissorEnabled) {
                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
                } else {
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                }
                GlStateManager.setActiveTexture(activeTexture);
                GlStateManager.bindTexture(boundTexture);
                GlStateManager.color(red, green, blue, alpha);
            }
        } catch (Exception ignored) {
        }
    }

    private static void renderBlurPass(int texture, Framebuffer target, int targetW, int targetH,
                                       int screenWidth, int screenHeight, float directionX, float directionY,
                                       float radius, boolean clipToPanel, float x, float y, float width,
                                       float height, int padding) {
        target.framebufferClear();
        target.bindFramebuffer(true);
        GlStateManager.viewport(0, 0, targetW, targetH);
        if (clipToPanel) {
            setPanelScissor(x, y, width, height, targetW, targetH, screenWidth, screenHeight, padding);
        }
        GL20.glUseProgram(program);
        GL20.glUniform1i(textureUniform, 0);
        GL20.glUniform2f(texelSizeUniform, 1.0F / targetW, 1.0F / targetH);
        GL20.glUniform2f(directionUniform, directionX, directionY);
        GL20.glUniform1f(radiusUniform, radius);
        GlStateManager.bindTexture(texture);
        drawQuad(screenWidth, screenHeight);
        GL20.glUseProgram(0);
        GlStateManager.bindTexture(0);
    }

    private static void setPanelScissor(float x, float y, float width, float height,
                                        int targetWidth, int targetHeight, int screenWidth,
                                        int screenHeight, int padding) {
        int left = (int) Math.floor(x * targetWidth / screenWidth) - padding;
        int right = (int) Math.ceil((x + width) * targetWidth / screenWidth) + padding;
        int top = (int) Math.floor(y * targetHeight / screenHeight) - padding;
        int bottom = (int) Math.ceil((y + height) * targetHeight / screenHeight) + padding;
        left = Math.max(0, left);
        top = Math.max(0, top);
        right = Math.min(targetWidth, right);
        bottom = Math.min(targetHeight, bottom);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left, targetHeight - bottom, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    private static void ensureResources() {
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        try {
            if (program == 0) {
                program = createProgram();
                if (program != 0) {
                    textureUniform = GL20.glGetUniformLocation(program, "textureIn");
                    texelSizeUniform = GL20.glGetUniformLocation(program, "texelSize");
                    directionUniform = GL20.glGetUniformLocation(program, "direction");
                    radiusUniform = GL20.glGetUniformLocation(program, "radius");
                }
            }
            if (roundedProgram == 0) {
                roundedProgram = createRoundedProgram();
                if (roundedProgram != 0) {
                    roundedTextureUniform = GL20.glGetUniformLocation(roundedProgram, "textureIn");
                    panelMinUniform = GL20.glGetUniformLocation(roundedProgram, "panelMin");
                    panelSizeUniform = GL20.glGetUniformLocation(roundedProgram, "panelSize");
                    cornerRadiusUniform = GL20.glGetUniformLocation(roundedProgram, "cornerRadius");
                }
            }

            int targetW = Math.max(1, mc.displayWidth / DOWNSAMPLE);
            int targetH = Math.max(1, mc.displayHeight / DOWNSAMPLE);

            if (horizontalBuffer == null || blurBuffer == null || mc.displayWidth != lastDisplayWidth || mc.displayHeight != lastDisplayHeight) {
                lastDisplayWidth = mc.displayWidth;
                lastDisplayHeight = mc.displayHeight;

                if (horizontalBuffer != null) {
                    try {
                        horizontalBuffer.deleteFramebuffer();
                    } catch (Exception ignored) {}
                }
                if (blurBuffer != null) {
                    try {
                        blurBuffer.deleteFramebuffer();
                    } catch (Exception ignored) {}
                }

                horizontalBuffer = new Framebuffer(targetW, targetH, false);
                blurBuffer = new Framebuffer(targetW, targetH, false);
                setLinearFiltering();
            }
        } finally {
            mc.getFramebuffer().bindFramebuffer(true);
            GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
            GlStateManager.setActiveTexture(activeTexture);
            GlStateManager.bindTexture(boundTexture);
            if (depthEnabled) {
                GlStateManager.enableDepth();
            } else {
                GlStateManager.disableDepth();
            }
        }
    }

    private static void setLinearFiltering() {
        if (horizontalBuffer != null) horizontalBuffer.setFramebufferFilter(GL11.GL_LINEAR);
        if (blurBuffer != null) blurBuffer.setFramebufferFilter(GL11.GL_LINEAR);
    }

    private static int createProgram() {
        String vertex = "#version 120\n"
                + "void main() {\n"
                + "    gl_TexCoord[0] = gl_MultiTexCoord0;\n"
                + "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n"
                + "}\n";
        String fragment = "#version 120\n"
                + "uniform sampler2D textureIn;\n"
                + "uniform vec2 texelSize;\n"
                + "uniform vec2 direction;\n"
                + "uniform float radius;\n"
                + "void main() {\n"
                + "    vec2 uv = gl_TexCoord[0].st;\n"
                + "    float sigma = max(radius * 0.65, 1.5);\n"
                + "    vec4 center = texture2D(textureIn, uv);\n"
                + "    vec3 rgb = center.rgb * center.a;\n"
                + "    float alpha = center.a;\n"
                + "    float total = 1.0;\n"
                + "    for (int i = 1; i <= 7; i++) {\n"
                + "        float fi = float(i);\n"
                + "        float weight = exp(-(fi * fi) / (2.0 * sigma * sigma));\n"
                + "        vec2 offset = direction * texelSize * (fi * 1.8);\n"
                + "        vec4 sample1 = texture2D(textureIn, uv + offset);\n"
                + "        vec4 sample2 = texture2D(textureIn, uv - offset);\n"
                + "        rgb += sample1.rgb * sample1.a * weight;\n"
                + "        rgb += sample2.rgb * sample2.a * weight;\n"
                + "        alpha += (sample1.a + sample2.a) * weight;\n"
                + "        total += 2.0 * weight;\n"
                + "    }\n"
                + "    rgb /= max(total, 0.001);\n"
                + "    alpha /= max(total, 0.001);\n"
                + "    gl_FragColor = vec4(rgb / max(alpha, 0.001), alpha);\n"
                + "}\n";

        int vShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vShader, vertex);
        GL20.glCompileShader(vShader);
        if (GL20.glGetShaderi(vShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GL20.glDeleteShader(vShader);
            return 0;
        }

        int fShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fShader, fragment);
        GL20.glCompileShader(fShader);
        if (GL20.glGetShaderi(fShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GL20.glDeleteShader(vShader);
            GL20.glDeleteShader(fShader);
            return 0;
        }

        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vShader);
        GL20.glAttachShader(prog, fShader);
        GL20.glLinkProgram(prog);
        GL20.glDeleteShader(vShader);
        GL20.glDeleteShader(fShader);

        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            GL20.glDeleteProgram(prog);
            return 0;
        }
        return prog;
    }

    private static int createRoundedProgram() {
        String vertex = "#version 120\n"
                + "void main() {\n"
                + "    gl_TexCoord[0] = gl_MultiTexCoord0;\n"
                + "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n"
                + "}\n";
        String fragment = "#version 120\n"
                + "uniform sampler2D textureIn;\n"
                + "uniform vec2 panelMin;\n"
                + "uniform vec2 panelSize;\n"
                + "uniform float cornerRadius;\n"
                + "void main() {\n"
                + "    vec2 halfSize = panelSize * 0.5;\n"
                + "    vec2 q = abs(gl_FragCoord.xy - (panelMin + halfSize)) - (halfSize - vec2(cornerRadius));\n"
                + "    float distanceToCorner = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - cornerRadius;\n"
                + "    float coverage = 1.0 - smoothstep(-1.0, 1.0, distanceToCorner);\n"
                + "    if (coverage <= 0.0) discard;\n"
                + "    vec4 color = texture2D(textureIn, gl_TexCoord[0].st);\n"
                + "    gl_FragColor = vec4(color.rgb, color.a * coverage);\n"
                + "}\n";

        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertex);
        GL20.glCompileShader(vertexShader);
        if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GL20.glDeleteShader(vertexShader);
            return 0;
        }

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragment);
        GL20.glCompileShader(fragmentShader);
        if (GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            return 0;
        }

        int roundedProgram = GL20.glCreateProgram();
        GL20.glAttachShader(roundedProgram, vertexShader);
        GL20.glAttachShader(roundedProgram, fragmentShader);
        GL20.glLinkProgram(roundedProgram);
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        if (GL20.glGetProgrami(roundedProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            GL20.glDeleteProgram(roundedProgram);
            return 0;
        }
        return roundedProgram;
    }

    private static void drawQuad(int screenWidth, int screenHeight) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(0.0F, 0.0F);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(0.0F, screenHeight);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f(screenWidth, screenHeight);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f(screenWidth, 0.0F);
        GL11.glEnd();
    }

    private static void restoreCapability(int cap, boolean enabled) {
        if (enabled) {
            GL11.glEnable(cap);
        } else {
            GL11.glDisable(cap);
        }
    }
}

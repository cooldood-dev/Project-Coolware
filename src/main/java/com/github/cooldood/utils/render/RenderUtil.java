package com.github.cooldood.utils.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RenderTickEvent;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.utils.client.C;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.FloatBuffer;

public class RenderUtil {
    public static net.minecraft.client.shader.Framebuffer createFrameBuffer(net.minecraft.client.shader.Framebuffer framebuffer) {
        return createFrameBuffer(framebuffer, false);
    }
    public static net.minecraft.client.shader.Framebuffer createFrameBuffer(net.minecraft.client.shader.Framebuffer framebuffer, boolean depth) {
        if (framebuffer == null || framebuffer.framebufferWidth != net.minecraft.client.Minecraft.getMinecraft().displayWidth || framebuffer.framebufferHeight != net.minecraft.client.Minecraft.getMinecraft().displayHeight) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            return new net.minecraft.client.shader.Framebuffer(net.minecraft.client.Minecraft.getMinecraft().displayWidth, net.minecraft.client.Minecraft.getMinecraft().displayHeight, depth);
        }
        return framebuffer;
    }
    public static void bindTexture(int texture) {
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, texture);
    }
    public static void setAlphaLimit(float limit) {
        net.minecraft.client.renderer.GlStateManager.enableAlpha();
        net.minecraft.client.renderer.GlStateManager.alphaFunc(org.lwjgl.opengl.GL11.GL_GREATER, (float) (limit * .01));
    }

    public static RenderSide renderSide = RenderSide.Tick;

    public static int ticks = 0;

    @SubscribeEvent
    public static void onRenderTick(RenderTickEvent event) {
        ticks++;
    }

    public enum RenderSide {
        GUI,
        Tick,
        World,
        Else
    }

    // do this before rendering 2d stuff!
    public static void beginRender() {
        GL11.glPushMatrix();

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GlStateManager.disableAlpha();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.disableTexture2D();
    }

    public static void finishRender() {
        getTessalator().draw();

        resetRender();
    }

    public static void resetRender() {
        GL11.glPopMatrix();

        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_FLAT);

        GlStateManager.enableAlpha();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GlStateManager.enableTexture2D();
    }

    public static Tessellator getTessalator() {
        return Tessellator.getInstance();
    }

    private static WorldRenderer getWorldRenderer() {
        return getTessalator().getWorldRenderer();
    }

    public static void glColor(Color color) {
        GlStateManager.color(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, color.getAlpha()/255f);
    }

    public static void glTranslate(Vec3 vec3) {
        GL11.glTranslated(vec3.xCoord, vec3.yCoord, vec3.zCoord);
    }

    // used ai to create this scissor function, as intellij ultimate intended.
    public static void glScissor(float x, float y, float width, float height) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        // Extract the model-view transformation matrix
        float scaleFactor = C.res().getScaleFactor();

        // Convert the local x, y coordinates into window space after rotations
        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);

        // Get the current matrix values for transformation (extract position and rotation effect)
        float m00 = matrix.get(0), m01 = matrix.get(4), m03 = matrix.get(12); // X-axis
        float m10 = matrix.get(1), m11 = matrix.get(5), m13 = matrix.get(13); // Y-axis

        // Transform the four corners of the scissor box
        float x1 = x * m00 + y * m01 + m03;
        float y1 = x * m10 + y * m11 + m13;
        float x2 = (x + width) * m00 + y * m01 + m03;
        float y2 = (x + width) * m10 + y * m11 + m13;
        float x3 = (x + width) * m00 + (y + height) * m01 + m03;
        float y3 = (x + width) * m10 + (y + height) * m11 + m13;
        float x4 = x * m00 + (y + height) * m01 + m03;
        float y4 = x * m10 + (y + height) * m11 + m13;

        // Calculate the bounding box for the rotated rectangle
        float scissorX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
        float scissorY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
        float scissorWidth = Math.max(Math.max(x1, x2), Math.max(x3, x4)) - scissorX;
        float scissorHeight = Math.max(Math.max(y1, y2), Math.max(y3, y4)) - scissorY;

        // Adjust for OpenGL scissor box, which uses bottom-left origin
        float adjustedX = scissorX * scaleFactor;
        float adjustedY = scissorY * scaleFactor;
        float adjustedWidth = scissorWidth * scaleFactor;
        float adjustedHeight = scissorHeight * scaleFactor;

        // Ensure the scissor box does not go negative and respects bounds
        adjustedWidth = Math.max(adjustedWidth, 0);
        adjustedHeight = Math.max(adjustedHeight, 0);

        // Configure the scissor rectangle with transformed values
        GL11.glScissor(
                (int) Math.ceil(adjustedX),
                (int) Math.ceil(C.mc.displayHeight - (adjustedY + adjustedHeight)),
                (int) Math.ceil(adjustedWidth),
                (int) Math.ceil(adjustedHeight)
        );
    }


    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }


    private static final FloatBuffer TRANSLATIONS_BUFFER = BufferUtils.createFloatBuffer(16);

    public static float[] getCurrentTranslation() {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, TRANSLATIONS_BUFFER);
        return new float[] {
                TRANSLATIONS_BUFFER.get(12), // X translation
                TRANSLATIONS_BUFFER.get(13), // Y translation
                TRANSLATIONS_BUFFER.get(14), // Z translation

                TRANSLATIONS_BUFFER.get(0),  // X scale
                TRANSLATIONS_BUFFER.get(5)  // Y scale
        };
    }

    protected static WorldRenderer worldRenderer;

    public static void beginAddingVertex(int DrawMode, VertexFormat vertexFormat) {
        worldRenderer = getWorldRenderer();
        worldRenderer.begin(DrawMode, vertexFormat);
    }

    public static void addVertex(double x, double y) {
        addVertex(x,y,0);
    }

    public static void addVertex(double x, double y, double z) {
        worldRenderer.pos(x, y, z).endVertex();
    }

    public static void addVertexColor(float x, float y, Color color) {
        worldRenderer.pos(x, y, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
    }

    public static void addVertexTexture(float x, float y, float textureX, float textureY) {
        worldRenderer.pos(x, y, 0).tex(textureX, textureY).endVertex();
    }

    public static void addVertexTextureColor(float x, float y, Color color, float textureX, float textureY) {
        worldRenderer.pos(x, y, 0).tex(textureX, textureY).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
    }

    // actually rendering!

    public static void drawRect(double x, double y, double w, double h, Color color) {
        beginRender();
        beginAddingVertex(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        glColor(color);

        addVertex(x, y+h);
        addVertex(x+w, y+h);
        addVertex(x+w, y);
        addVertex(x, y);

        finishRender();
    }

    public static void drawRectOutline(double x, double y, double w, double h, float lineThickness, Color color) {
        beginRender();
        beginAddingVertex(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        glColor(color);
        GL11.glLineWidth(lineThickness);

        addVertex(x, y+h);
        addVertex(x+w, y+h);
        addVertex(x+w, y);
        addVertex(x, y);

        finishRender();
    }

    public static void drawArrow(float x, float y, float w, float h, boolean up,  int lineThickness, Color color) {
        x = (int) x;
        y = (int) (up ? y + h : y);
        w = (int) w;
        h = (int) (up ? -h : h);

        beginRender();
        beginAddingVertex(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        glColor(color);
        GL11.glLineWidth(lineThickness);

        addVertex(x, y);
        addVertex(x + w / 2, y + h);
        addVertex(x + w, y);

        finishRender();
    }

    public static void drawGradient(float x, float y, float w, float h, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        beginRender();
        beginAddingVertex(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        addVertexColor(x, y+h, bottomLeft);
        addVertexColor(x+w, y+h, bottomRight);
        addVertexColor(x+w, y, topRight);
        addVertexColor(x, y, topLeft);

        finishRender();
    }

    public static void drawGradientLR(float x, float y, float w, float h, Color leftColor, Color rightColor) {
        drawGradient(x, y, w, h, leftColor, leftColor, rightColor, rightColor);
    }

    public static void drawGradientTB(float x, float y, float w, float h, Color topColour, Color bottomColour) {
        drawGradient(x, y, w, h, bottomColour, topColour, bottomColour, topColour);
    }

    public static void bindTexture(ResourceLocation texture) {
        C.mc.getTextureManager().bindTexture(texture);
    }

    public static void drawPlayerHead(float x, float y, float w, float h, Color color, EntityLivingBase entity) {
        glColor(color);

        ResourceLocation skinTexture = new ResourceLocation("textures/entity/steve.png");
        if (entity instanceof AbstractClientPlayer) {
            skinTexture = ((AbstractClientPlayer) entity).getLocationSkin();
        }
        RenderUtil.bindTexture(skinTexture);

        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 8, 8,  8, 8, (int) w, (int) h, 64, 64);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 40, 8, 8, 8, (int) w, (int) h, 64, 64);
    }

    public static void drawRectTextured(float x, float y, float w, float h, Color color, int textureID) {
        GlStateManager.bindTexture(textureID);
        drawRectTextured(x, y, w, h, color);
    }

    public static void drawRectTextured(float x, float y, float w, float h, Color color) {
        beginRender();
        GlStateManager.enableTexture2D();
        RenderUtil.beginAddingVertex(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        glColor(color);

        addVertexTexture(x, y+h, 0, 1);
        addVertexTexture(x+w, y+h, 1, 1);
        addVertexTexture(x+w, y, 1, 0);
        addVertexTexture(x, y, 0, 0);

        finishRender();
    }

    public static void drawRectTexturedColor(float x, float y, float w, float h, Color color1, Color color2, int textureID) {
        beginRender();
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(textureID);
        RenderUtil.beginAddingVertex(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        addVertexTextureColor(x, y+h, color1, 0, 1);
        addVertexTextureColor(x+w, y+h, color2, 1, 1);
        addVertexTextureColor(x+w, y, color2, 1, 0);
        addVertexTextureColor(x, y, color1, 0, 0);

        finishRender();
    }

    public static void drawRectSprite(float x, float y, float w, float h, Color color, TextureAtlasSprite sprite) {
        beginRender();
        GlStateManager.enableTexture2D();
        RenderUtil.beginAddingVertex(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        glColor(color);

        addVertexTexture(x, y+h, sprite.getMinU(), sprite.getMinV());
        addVertexTexture(x+w, y+h, sprite.getMaxU(), sprite.getMinV());
        addVertexTexture(x+w, y, sprite.getMaxU(), sprite.getMaxV());
        addVertexTexture(x, y, sprite.getMinU(), sprite.getMaxV());

        finishRender();
    }

    public static void drawRoundedRect(float x, float y, float w, float h, float radius, Color color) {
        drawRoundedRect(x,y,w,h,radius,color, true, true, true, true);
    }

    /// the booleans determine if each edge should be rounded.
    public static void drawRoundedRect(float x, float y, float w, float h, float radius, Color color, boolean topLeft, boolean bottomLeft, boolean bottomRight, boolean topRight) {
        // makes sure the rectangle doesn't bend in on itself
        radius = Math.min(radius, w / 2);
        radius = Math.min(radius, h / 2);

        // setup drawing
        beginRender();
        beginAddingVertex(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        glColor(color);

        // loops through a whole circle, every 90 degrees move to a corner
        // 0 is top left, 90 is bottom left, 180 is bottom right, 270 is top right
        for (int i = 0; i <= 360; i+=5) {
            float xCornerOffset = (i >= 180 ? w-(radius*2) : 0);
            float yCornerOffset = (i >= 90 && i <= 270 ? h-(radius*2) : 0);

            float xN = (float) (x + radius + (Math.sin((i * Math.PI) / 180)) * (radius * -1)) + xCornerOffset;
            float yN = (float) (y + radius + (Math.cos((i * Math.PI) / 180)) * (radius * -1)) + yCornerOffset;

            if (!topLeft && i == 0)         {   xN = x;         yN = y;         i+=89;}
            if (!bottomLeft && i == 90)     {   xN = x;         yN = y + h;     i+=89;}
            if (!bottomRight && i == 180)   {   xN = x + w;     yN = y + h;     i+=89;}
            if (!topRight && i == 270)      {   xN = x + w;     yN = y;         i+=89;}


            addVertex(xN, yN);
        }

        // we have finished! draw!
        finishRender();
    }

    public static void drawRoundedRectOutline(float x, float y, float w, float h, float radius, float lineWidth, Color color) {
        drawRoundedRectOutline(x,y,w,h,radius,lineWidth,color, true, true, true, true);
    }

    public static void drawRoundedRectOutline(float x, float y, float w, float h, float radius, float lineWidth, Color color, boolean topLeft, boolean bottomLeft, boolean bottomRight, boolean topRight) {
        // makes sure the rectangle doesn't bend in on itself
        radius = Math.min(radius, w / 2);
        radius = Math.min(radius, h / 2);

        // setup drawing
        beginRender();
        beginAddingVertex(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        GL11.glLineWidth(lineWidth);
        glColor(color);

        // loops through a whole circle, every 90 degrees move to a corner
        // 0 is top left, 90 is bottom left, 180 is bottom right, 270 is top right

        // looping 360 times may poop on fps, maybe i+=5 would help,
        for (int i = 0; i <= 360; i+=5) {
            float xCornerOffset = (i >= 180 ? w-(radius*2) : 0);
            float yCornerOffset = (i >= 90 && i <= 270 ? h-(radius*2) : 0);

            float xN = (float) (x + radius + (Math.sin((i * Math.PI) / 180)) * (radius * -1)) + xCornerOffset;
            float yN = (float) (y + radius + (Math.cos((i * Math.PI) / 180)) * (radius * -1)) + yCornerOffset;

            if (!topLeft && i == 0)         {   xN = x;         yN = y;         i+=89;}
            if (!bottomLeft && i == 90)     {   xN = x;         yN = y + h;     i+=89;}
            if (!bottomRight && i == 180)   {   xN = x + w;     yN = y + h;     i+=89;}
            if (!topRight && i == 270)      {   xN = x + w;     yN = y;         i+=89;}


            addVertex(xN, yN);
        }

        // we have finished! draw!
        finishRender();
    }

    public static void drawRoundedRectOutline(float x, float y, float w, float h, float radius, float lineWidth, Color colour1, Color colour2, Color colour3, Color colour4) {
        // makes sure the rectangle doesn't bend in on itself
        radius = Math.min(radius, w / 2);
        radius = Math.min(radius, h / 2);

        // setup drawing
        beginRender();
        beginAddingVertex(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth(lineWidth);

        // loops through a whole circle, every 90 degrees move to a corner
        // 0 is top left, 90 is bottom left, 180 is bottom right, 270 is top right

        // looping 360 times may poop on fps, maybe i+=5 would help,
        for (int i = 0; i <= 360; i+=5) {
            Color colour = i > 270 ? colour4 : i >= 180 ? colour3 : i >= 90 ? colour2 : colour1;

            float xCornerOffset = (i >= 180 ? w-(radius*2) : 0);
            float yCornerOffset = (i >= 90 && i <= 270 ? h-(radius*2) : 0);

            float xN = (float) (x + radius + (Math.sin((i * Math.PI) / 180)) * (radius * -1)) + xCornerOffset;
            float yN = (float) (y + radius + (Math.cos((i * Math.PI) / 180)) * (radius * -1)) + yCornerOffset;

            addVertexColor(xN, yN, colour);
        }

        addVertexColor(x + radius, y, colour1);

        // we have finished! draw!
        finishRender();
    }

    public static Color getProgressColour(float percent) {
        percent = MathHelper.clamp_float(percent, 0, 1);
        int progressInt = (int) (255 * MathHelper.clamp_float((float) EasingUtil.EasingFunctions.Ease_In_Out_Sine.ease(percent), 0, 1));
        return new Color(255-progressInt, progressInt, 80);
    }

    public static void drawRoundedRectFade(float x, float y, float w, float h, float radius, Color color1, Color color2) {
        // makes sure the rectangle doesn't bend in on itself
        radius = Math.min(radius, w / 2);
        radius = Math.min(radius, h / 2);

        // setup drawing
        beginRender();
        beginAddingVertex(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        // loops through a whole circle, every 90 degrees move to a corner
        // 0 is top left, 90 is bottom left, 180 is bottom right, 270 is top right

        // looping 360 times may poop on fps, maybe i+=5 would help,
        for (int i = 0; i <= 360; i+=5) {
            float xCornerOffset = (i > 180 ? w-(radius*2) : 0);
            float yCornerOffset = (i > 90 && i <= 270 ? h-(radius*2) : 0);

            addVertexColor(
                    (float) (x + radius + (Math.sin((i * Math.PI) / 180)) * (radius * -1)) + xCornerOffset,
                    (float) (y + radius + (Math.cos((i * Math.PI) / 180)) * (radius * -1)) + yCornerOffset,
                    i <= 180 ? color1 : color2
            );
        }

        // we have finished! draw!
        finishRender();
    }

    public static void drawRoundedRectGlow(float x, float y, float w, float h, float radius, Color color) {
        // makes sure the rectangle doesn't bend in on itself
        radius = Math.min(radius, w / 2);
        radius = Math.min(radius, h / 2);

        // setup drawing
        beginRender();
        beginAddingVertex(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        addVertexColor(
                x + (w/2),
                y + (h/2),
                color
        );

        for (int i = 0; i <= 360; i+=10) {
            float xCornerOffset = (i > 180 ? w-(radius*2) : 0);
            float yCornerOffset = (i > 90 && i <= 270 ? h-(radius*2) : 0);

            addVertexColor(
                    (float) (x + radius + (Math.sin((i * Math.PI) / 180)) * (radius * -1)) + xCornerOffset,
                    (float) (y + radius + (Math.cos((i * Math.PI) / 180)) * (radius * -1)) + yCornerOffset,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
            );
        }


        addVertexColor(
                x + radius,
                y,
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
        );

        // we have finished! draw!
        finishRender();
    }

    // this is probably stupid, but i dont know how to make shaders and never will probably.
    // Cache blur colors to prevent allocations in loop
    private static final Color[] BLUR_COLORS = new Color[50];
    static {
        for (int i = 0; i < BLUR_COLORS.length; i++) {
            BLUR_COLORS[i] = new Color(1, 1, 1, 1 / (i + 1f));
        }
    }

    public static void drawBlurRect(float x, float y, float w, float h, int iterations) {
        float[] translation = RenderUtil.getCurrentTranslation();
        int screenTexture = getScreenTexture();

        beginRender();
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(screenTexture);

        RenderUtil.beginAddingVertex(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        GlStateManager.colorMask(true, true, true, false);

        // need the current translations for uv mapping!
        float realX = (x + translation[0]) / translation[3];
        float realY = (y + translation[1]) / translation[4];

        float minU = (realX / C.res().getScaledWidth());
        float minV = 1 - (realY / C.res().getScaledHeight());
        float maxU = ((w + realX) / C.res().getScaledWidth());
        float maxV = 1 - ((h + realY) / C.res().getScaledHeight());

        for (int j = 0; j <= iterations; j++) {
            float f1 = (j - iterations / 2f) / 1024f;
            Color colour = j < BLUR_COLORS.length ? BLUR_COLORS[j] : new Color(1, 1, 1, 1 / (j + 1f));

            addVertexTextureColor(
                    x, y + h,
                    colour,
                    minU + f1, maxV + f1
            );
            addVertexTextureColor(
                    x + w, y + h,
                    colour,
                    maxU + f1, maxV + f1
            );
            addVertexTextureColor(
                    x + w, y,
                    colour,
                    maxU + f1, minV + f1
            );
            addVertexTextureColor(
                    x, y,
                    colour,
                    minU + f1, minV + f1
            );
        }

        finishRender();
        GlStateManager.colorMask(true, true, true, true);
        // Do not delete texture! It's cached now for performance.
    }

    private static int cachedScreenTexture = -1;
    private static int cachedScreenWidth = -1;
    private static int cachedScreenHeight = -1;

    @com.github.cooldood.events.SubscribeEvent
    public static void onWindowResize(com.github.cooldood.events.impl.WindowResizeEvent event) {
        cachedScreenTexture = -1;
        cachedScreenWidth = -1;
        cachedScreenHeight = -1;
    }

    public static int getScreenTexture() {
        if (cachedScreenTexture == -1 || cachedScreenWidth != C.mc.displayWidth || cachedScreenHeight != C.mc.displayHeight) {
            if (cachedScreenTexture != -1) {
                GlStateManager.deleteTexture(cachedScreenTexture);
            }
            cachedScreenTexture = GlStateManager.generateTexture();
            cachedScreenWidth = C.mc.displayWidth;
            cachedScreenHeight = C.mc.displayHeight;
            GlStateManager.bindTexture(cachedScreenTexture);

            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    C.mc.displayWidth,
                    C.mc.displayHeight,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    (java.nio.ByteBuffer) null
            );

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        }

        GlStateManager.bindTexture(C.mc.getFramebuffer().framebufferTexture);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, cachedScreenTexture);
        GL11.glCopyTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0, 0,
                0, 0,
                C.mc.displayWidth,
                C.mc.displayHeight
        );

        return cachedScreenTexture;
    }

    @AllArgsConstructor
    public enum ThemeColours {
        Gray(new Color[] {new Color(150, 150, 150), new Color(120, 120, 120)}),
        Gay(new Color[] {new Color(255, 0, 0), new Color(255, 145, 0), new Color(255, 255, 0), new Color(0, 255, 0), new Color(0, 0, 255), new Color(150, 0, 255)}),
        Trans(new Color[] {new Color(91, 206, 250), new Color(245, 169, 184), new Color(255, 255, 255), new Color(245, 169, 184)}),
        Lesbian(new Color[] {new Color(213, 45, 0), new Color(239, 118, 39), new Color(255, 154, 86), new Color(255, 255, 255), new Color(209, 98, 164), new Color(181, 86, 144), new Color(163, 2, 98)}),
        Bi(new Color[] {new Color(214, 2, 112), new Color(214, 2, 112), new Color(155, 79, 150), new Color(0, 56, 168), new Color(0, 56, 168),}),
        Custom(new Color[] {}) {
            @Override
            public Color[] getColours() {
                return new Color[] {ThemeModule.primaryColor, ThemeModule.primaryColor};
            }
        };

        @Getter
        private final Color[] colours;
    }

    public static Color interpolateColors(Color color1, Color color2, double percent) {
        return new Color(
                (int) ((color1.getRed() * (1d-percent)) + (color2.getRed() * percent)),
                (int) ((color1.getGreen() * (1d-percent)) + (color2.getGreen() * percent)),
                (int) ((color1.getBlue() * (1d-percent)) + (color2.getBlue() * percent)),
                (int) ((color1.getAlpha() * (1d-percent)) + (color2.getAlpha() * percent))
        );
    }

    public static double getColorsFadeValue(double value, Color[] colours, double fadeSpeed) {
        double time = System.currentTimeMillis() * fadeSpeed * 0.001;
        double fadeValue = value * -0.001;

        return (time + fadeValue) % colours.length;
    }

    public static Color getColorsFade(double value, Color[] colours, double fadeSpeed) {
        double fadeTimeStart = getColorsFadeValue(value, colours, fadeSpeed);

        return interpolateColors(
                colours[(int) fadeTimeStart],
                colours[((int) fadeTimeStart + 1) % colours.length],
                fadeTimeStart % 1
        );
    }

    public static Color[] getColorsFade(double start, double width, Color[] colours, double fadeSpeed) {
        return new Color[] {
                getColorsFade(start, colours, fadeSpeed),
                getColorsFade(start+width, colours, fadeSpeed)
        };
    }

    public static void getColorsFade(double start, double width, Color[] colours, double fadeSpeed, Color[] out) {
        out[0] = getColorsFade(start, colours, fadeSpeed);
        out[1] = getColorsFade(start+width, colours, fadeSpeed);
    }


    public static Color getOppositeColour(Color colour) {
        return new Color(255-colour.getRed(), 255-colour.getGreen(), 255-colour.getBlue(), colour.getAlpha());
    }

    public static Color setOpacity(Color color, double opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (opacity * 255));
    }
}
package coolware.util.render.img;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.glColor4f;

public class ImageObject {

    public BufferedImage img = null;
    public int textureID = 0;
    private int width, height = 0;
    public boolean loading = false;
    public boolean isLoaded = false;
    private final ResourceLocation resourceLocation;
    private final File file;

    public ImageObject(File file) {
        this.file = file;
        this.resourceLocation = null;
    }

    public ImageObject(ResourceLocation resourceLocation) {
        this.file = null;
        this.resourceLocation = resourceLocation;
    }

    private void loadImage() throws IOException {
        if (resourceLocation != null) {
            loadImageFromResource();
        } else if (file != null) {
            loadImageFromDisk();
        }
    }

    private void loadImageFromDisk() throws IOException {
        img = ImageIO.read(file);
    }

    private void loadImageFromResource() throws IOException {
        InputStream stream = null;
        try {
            try {
                stream = Minecraft.getMinecraft().getResourceManager().getResource(resourceLocation).getInputStream();
            } catch (Throwable ignored) {
                stream = ImageObject.class.getResourceAsStream("/assets/minecraft/" + resourceLocation.getResourcePath());
            }
            if (stream == null) {
                stream = ImageObject.class.getResourceAsStream("/assets/coolware/" + resourceLocation.getResourcePath());
            }
            if (stream != null) {
                img = ImageIO.read(stream);
            }
        } finally {
            if (stream != null) {
                try { stream.close(); } catch (Throwable ignored) {}
            }
        }
    }

    public void load() {
        if (isLoaded) return;
        try {
            loadImage();
            if (img == null) return;
            width = img.getWidth();
            height = img.getHeight();

            int[] pixels = new int[img.getWidth() * img.getHeight()];
            img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
            ByteBuffer buffer = ByteBuffer.allocateDirect(img.getWidth() * img.getHeight() * 4);

            for (int h = 0; h < img.getHeight(); h++) {
                for (int w = 0; w < img.getWidth(); w++) {
                    int pixel = pixels[h * img.getWidth() + w];
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }
            buffer.flip();

            textureID = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, img.getWidth(), img.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            isLoaded = true;
        } catch (Throwable t) {
            // failed to load image
        }
    }

    public void loadAsync() {
        if (loading || isLoaded) return;
        loading = true;
        CompletableFuture.runAsync(() -> {
            try {
                loadImage();
                if (img == null) return;
                width = img.getWidth();
                height = img.getHeight();

                int[] pixels = new int[img.getWidth() * img.getHeight()];
                img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
                ByteBuffer buffer = ByteBuffer.allocateDirect(img.getWidth() * img.getHeight() * 4);

                for (int h = 0; h < img.getHeight(); h++) {
                    for (int w = 0; w < img.getWidth(); w++) {
                        int pixel = pixels[h * img.getWidth() + w];
                        buffer.put((byte) ((pixel >> 16) & 0xFF));
                        buffer.put((byte) ((pixel >> 8) & 0xFF));
                        buffer.put((byte) (pixel & 0xFF));
                        buffer.put((byte) ((pixel >> 24) & 0xFF));
                    }
                }
                buffer.flip();

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    textureID = GL11.glGenTextures();
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, img.getWidth(), img.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

                    isLoaded = true;
                });
            } catch (Throwable ignored) {
            }
        });
    }

    public void unload() {
        if (!isLoaded) return;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlStateManager.deleteTexture(textureID);
        isLoaded = false;
        loading = false;
    }

    public void drawImg(float x, float y, float width, float height) {
        if (!isLoaded) {
            loadAsync();
            return;
        }

        enableTexture2D();
        enableBlend();
        glColor4f(1, 1, 1, 1);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        Gui.drawModalRectWithCustomSizedTexture((int) x, (int) y, 0, 0, (int) width, (int) height, width, height);
        resetColor();
    }

    public void drawImg(float x, float y, float width, float height, Color color) {
        if (!isLoaded) {
            loadAsync();
            return;
        }

        enableTexture2D();
        enableBlend();
        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        Gui.drawModalRectWithCustomSizedTexture((int) x, (int) y, 0, 0, (int) width, (int) height, width, height);
        resetColor();
    }

    public void drawImg(float x, float y) {
        drawImg(x, y, width, height);
    }
}

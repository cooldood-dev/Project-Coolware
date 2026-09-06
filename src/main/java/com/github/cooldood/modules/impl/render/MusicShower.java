package com.github.cooldood.modules.impl.render;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.WindowsMediaProvider;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.IconFont;
import com.github.cooldood.utils.render.RenderUtil;
import com.github.cooldood.utils.render.draggable.Draggable;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

@RegisterModule(
        name = "Music Shower",
        description = "Shows currently playing Windows music with spinning vinyl & album cover.",
        category = Category.RENDER,
        enabledByDefault = true
)
public class MusicShower extends Module {

    @RegisterSubModule(name = "Spin Speed", min = 0.5, max = 5.0, increment = 0.5)
    public static double spinSpeed = 2.0;

    @RegisterSubModule(name = "Show Vinyl", description = "Render spinning vinyl disc")
    public static boolean showVinyl = true;

    @RegisterSubModule(name = "Show Visualizer", description = "Render animated soundwave equalizer bars")
    public static boolean showVisualizer = true;

    private static float vinylRotation = 0f;
    private static long lastRenderTime = System.currentTimeMillis();

    private static final float CORNER_RAD = 6f;
    private static final float CARD_H = 46f;
    private static final float MIN_CARD_W = 160f;

    public static Draggable musicShowerDraggable = new Draggable(
            "MusicShowerHUD",
            () -> {
                long now = System.currentTimeMillis();
                float delta = (now - lastRenderTime) / 1000f;
                lastRenderTime = now;

                WindowsMediaProvider.updateGlTexture();
                WindowsMediaProvider.MediaInfo media = WindowsMediaProvider.getCurrentMedia();

                boolean isPreview = C.mc.currentScreen instanceof com.github.cooldood.screens.ClickGUI.ClickGUIScreen;

                if (!media.isPlaying && !isPreview) {
                    return new double[]{0, 0};
                }

                // If playing, update vinyl spin
                if (media.isPlaying || isPreview) {
                    vinylRotation += (float) (delta * 60f * spinSpeed);
                    if (vinylRotation >= 360f) vinylRotation -= 360f;
                }

                String title = media.isPlaying ? media.title : "No Music Playing";
                String artist = media.isPlaying ? (media.artist.isEmpty() ? "Unknown Artist" : media.artist) : "Play a track on Windows";

                if (title.isEmpty()) title = "Playing Audio";

                Color accent = ThemeModule.getAccentColor();
                Color panelBg = ThemeModule.panelBackground;
                Color textDim = new Color(180, 180, 195, 255);

                // Dimensions
                float coverSize = 34f;
                float vinylRadius = showVinyl ? 17f : 0f;
                float pad = 6f;

                float maxTitleW = FontUtil.getStringWidth(title, 10);
                float maxArtistW = FontUtil.getStringWidth(artist, 8);
                float textBlockW = Math.max(maxTitleW, maxArtistW);

                float cardW = pad + coverSize + (showVinyl ? (vinylRadius * 0.9f) : 0f) + 8f + textBlockW + pad + 12f;
                if (cardW < MIN_CARD_W) cardW = MIN_CARD_W;

                // 1. Draw Card Background
                RenderUtil.drawRoundedRect(0, 0, cardW, CARD_H, CORNER_RAD, panelBg);
                RenderUtil.drawRoundedRect(2, 2, 2.5f, CARD_H - 4, 1, accent);

                // 2. Draw Vinyl Disc (half-tucked behind or beside the cover art)
                float coverX = pad + 2f;
                float coverY = (CARD_H - coverSize) / 2f;

                if (showVinyl) {
                    float vinylCenterX = coverX + coverSize - 2f;
                    float vinylCenterY = coverY + (coverSize / 2f);
                    drawSpinningVinyl(vinylCenterX, vinylCenterY, vinylRadius, vinylRotation, accent, media);
                }

                // 3. Draw Album Cover Box
                drawAlbumCover(coverX, coverY, coverSize, media, accent);

                // 4. Draw Song Info
                float textX = coverX + coverSize + (showVinyl ? (vinylRadius * 0.9f) : 0f) + 6f;
                float titleY = coverY + 2f;
                float artistY = titleY + FontUtil.getFontHeight(10) + 3f;

                // Music Note Icon
                IconFont.drawIcon(IconFont.PLAY_CIRCLE, textX, titleY + 1f, 8, accent);
                float iconOffset = IconFont.getWidth(IconFont.PLAY_CIRCLE, 8) + 4f;

                FontUtil.drawString(title, textX + iconOffset, titleY, 10, Color.WHITE, false);
                FontUtil.drawString(artist, textX, artistY, 8, textDim, false);

                // 5. Draw Animated Soundwave Visualizer Bars
                if (showVisualizer) {
                    float vizX = textX;
                    float vizY = artistY + FontUtil.getFontHeight(8) + 4f;
                    drawVisualizer(vizX, vizY, 35f, 5f, accent);
                }

                return new double[]{cardW, CARD_H};
            },
            e -> ModuleManager.isEnabled(MusicShower.class),
            e -> true
    );

    private static void drawAlbumCover(float x, float y, float size, WindowsMediaProvider.MediaInfo media, Color accent) {
        if (media.thumbTexture != null) {
            GlStateManager.color(1f, 1f, 1f, 1f);
            C.mc.getTextureManager().bindTexture(media.thumbTexture);
            GlStateManager.enableTexture2D();
            Gui.drawModalRectWithCustomSizedTexture((int) x, (int) y, 0, 0, (int) size, (int) size, size, size);
            RenderUtil.drawRectOutline(x, y, size, size, 1f, accent);
        } else {
            // Fallback cover placeholder with music icon
            RenderUtil.drawRoundedRect(x, y, size, size, 3f, new Color(30, 30, 38, 255));
            RenderUtil.drawRectOutline(x, y, size, size, 1f, accent);
            float iconX = x + (size / 2f) - (IconFont.getWidth(IconFont.STAR, 12) / 2f);
            float iconY = y + (size / 2f) - 6f;
            IconFont.drawIcon(IconFont.STAR, iconX, iconY, 12, accent);
        }
    }

    private static void drawSpinningVinyl(float cx, float cy, float radius, float angle, Color accent, WindowsMediaProvider.MediaInfo media) {
        GL11.glPushMatrix();
        GL11.glTranslated(cx, cy, 0);
        GL11.glRotatef(angle, 0, 0, 1);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        // 1. Vinyl Black Disc Body
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(0.10f, 0.10f, 0.12f, 1.0f);
        GL11.glVertex2d(0, 0);
        for (int i = 0; i <= 36; i++) {
            double a = (i * Math.PI * 2) / 36;
            GL11.glVertex2d(Math.cos(a) * radius, Math.sin(a) * radius);
        }
        GL11.glEnd();

        // 2. Realistic Vinyl Groove Rings
        GL11.glLineWidth(1.0f);
        GL11.glColor4f(0.22f, 0.22f, 0.26f, 0.8f);

        float[] grooveRadii = {0.85f, 0.72f, 0.58f};
        for (float gr : grooveRadii) {
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i < 36; i++) {
                double a = (i * Math.PI * 2) / 36;
                GL11.glVertex2d(Math.cos(a) * (radius * gr), Math.sin(a) * (radius * gr));
            }
            GL11.glEnd();
        }

        // 3. Center Label (Accent color)
        float labelRadius = radius * 0.38f;
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4ub((byte) accent.getRed(), (byte) accent.getGreen(), (byte) accent.getBlue(), (byte) 255);
        GL11.glVertex2d(0, 0);
        for (int i = 0; i <= 24; i++) {
            double a = (i * Math.PI * 2) / 24;
            GL11.glVertex2d(Math.cos(a) * labelRadius, Math.sin(a) * labelRadius);
        }
        GL11.glEnd();

        // 4. Center Spindle Hole (Dark center cutout)
        float holeRadius = radius * 0.12f;
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(0.06f, 0.06f, 0.08f, 1.0f);
        GL11.glVertex2d(0, 0);
        for (int i = 0; i <= 16; i++) {
            double a = (i * Math.PI * 2) / 16;
            GL11.glVertex2d(Math.cos(a) * holeRadius, Math.sin(a) * holeRadius);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private static void drawVisualizer(float x, float y, float width, float maxHeight, Color color) {
        int barCount = 7;
        float barW = 2.5f;
        float gap = (width - (barCount * barW)) / (barCount - 1);
        long time = System.currentTimeMillis();

        for (int i = 0; i < barCount; i++) {
            // Dynamic sine wave height calculation
            double freq = (time * 0.008) + (i * 1.2);
            float h = (float) ((Math.sin(freq) + 1.0) / 2.0) * maxHeight;
            if (h < 1.5f) h = 1.5f;

            float bx = x + (i * (barW + gap));
            float by = y + (maxHeight - h);
            RenderUtil.drawRoundedRect(bx, by, barW, h, 1f, color);
        }
    }

    @Override
    protected void onEnable() {
        WindowsMediaProvider.start();
    }

    @Override
    protected void onDisable() {
        WindowsMediaProvider.stop();
    }

    static {
        musicShowerDraggable.x = 0.01;
        musicShowerDraggable.y = 0.85;
    }
}

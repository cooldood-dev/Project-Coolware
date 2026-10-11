package coolware.gui.menu;

import coolware.module.visuals.Theme;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class WaveRenderer {

    /**
     * Renders multi-layer smooth sinusoidal CSS-style waves dynamic with Theme colors.
     */
    public static void renderWaves(int screenWidth, int screenHeight, float time) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Theme theme = Theme.getInstance();
        int accent1 = theme.getColor(0);
        int accent2 = theme.getColor(120);

        int baseWaveY = (int) (screenHeight * 0.70F);

        // Layer 1: Back wave (soft translucent ambient crimson/burgundy)
        drawSinWave(screenWidth, screenHeight, baseWaveY - 24, 18.0F, 0.010F, time * 0.7F, 0.18F, 0x44000000 | (accent1 & 0xFFFFFF), 0x3310050A);

        // Layer 2: Mid wave (dark ruby/wine)
        drawSinWave(screenWidth, screenHeight, baseWaveY - 10, 22.0F, 0.014F, -time * 1.0F + 40.0F, 0.32F, 0x77000000 | (accent2 & 0xFFFFFF), 0x55180812);

        // Layer 3: Front-mid wave (vibrant crimson)
        drawSinWave(screenWidth, screenHeight, baseWaveY + 8, 26.0F, 0.018F, time * 1.3F + 120.0F, 0.50F, 0xAA000000 | (accent1 & 0xFFFFFF), 0x77200A16);

        // Layer 4: Foreground wave (solid scarlet/crimson crest with deep dark body)
        drawSinWave(screenWidth, screenHeight, baseWaveY + 26, 28.0F, 0.022F, -time * 1.6F + 200.0F, 0.85F, 0xDD000000 | (accent2 & 0xFFFFFF), 0xCC14070E);

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private static void drawSinWave(int width, int height, int startY, float amplitude, float frequency, float offset, float alphaMultiplier, int crestColor, int bottomColor) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        float crR = (float) (crestColor >> 16 & 255) / 255.0F;
        float crG = (float) (crestColor >> 8 & 255) / 255.0F;
        float crB = (float) (crestColor & 255) / 255.0F;
        float crA = ((float) (crestColor >> 24 & 255) / 255.0F) * alphaMultiplier;

        float btR = (float) (bottomColor >> 16 & 255) / 255.0F;
        float btG = (float) (bottomColor >> 8 & 255) / 255.0F;
        float btB = (float) (bottomColor & 255) / 255.0F;
        float btA = ((float) (bottomColor >> 24 & 255) / 255.0F) * alphaMultiplier;

        wr.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);

        int step = 4;
        for (int x = 0; x <= width + step; x += step) {
            float waveY = startY + (float) Math.sin((x * frequency) + offset) * amplitude
                    + (float) Math.cos((x * frequency * 0.5F) + (offset * 0.7F)) * (amplitude * 0.35F);

            // Top vertex on the wave crest
            wr.pos((double) x, (double) waveY, 0.0D).color(crR, crG, crB, crA).endVertex();
            // Bottom vertex at bottom of screen
            wr.pos((double) x, (double) height, 0.0D).color(btR, btG, btB, btA).endVertex();
        }

        tessellator.draw();
    }
}

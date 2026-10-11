package coolware.gui.menu;

import coolware.module.visuals.Theme;
import coolware.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleSystem {
    private static final int MAX_PARTICLES = 70;
    private static final float MAX_CONNECT_DIST = 90.0F;
    private static final float MOUSE_CONNECT_DIST = 110.0F;

    private final List<Particle> particles = new ArrayList<Particle>();
    private final Random random = new Random();
    private int prevWidth = 0;
    private int prevHeight = 0;
    private long lastTime = 0;

    private static class Particle {
        float x, y;
        float vx, vy;
        float radius;
        float baseAlpha;
        float alphaPhase;
        float alphaSpeed;
        int color;

        Particle(float x, float y, float vx, float vy, float radius, float baseAlpha, float alphaSpeed, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
            this.baseAlpha = baseAlpha;
            this.alphaPhase = (float) (Math.random() * Math.PI * 2.0);
            this.alphaSpeed = alphaSpeed;
            this.color = color;
        }
    }

    public void init(int width, int height) {
        particles.clear();
        this.prevWidth = width;
        this.prevHeight = height;
        this.lastTime = System.currentTimeMillis();

        for (int i = 0; i < MAX_PARTICLES; i++) {
            particles.add(createParticle(width, height));
        }
    }

    private Particle createParticle(int width, int height) {
        float x = random.nextFloat() * (width > 0 ? width : 800);
        float y = random.nextFloat() * (height > 0 ? height : 600);
        float speed = 0.15F + random.nextFloat() * 0.35F;
        double angle = random.nextDouble() * Math.PI * 2.0;
        float vx = (float) Math.cos(angle) * speed;
        float vy = (float) Math.sin(angle) * speed;
        float radius = 1.0F + random.nextFloat() * 2.2F;
        float baseAlpha = 0.25F + random.nextFloat() * 0.45F;
        float alphaSpeed = 0.02F + random.nextFloat() * 0.03F;

        // Variety of Crimson / Ruby / Rose / White-pink tinted particles
        int[] palette = new int[] {
                0xFFFF3355, // vibrant ruby
                0xFFFF5577, // soft crimson
                0xFFFF8899, // bright coral pink
                0xFFFFAAAA, // pastel rose
                0xFFFFFFFF  // sparkling white
        };
        int col = palette[random.nextInt(palette.length)];

        return new Particle(x, y, vx, vy, radius, baseAlpha, alphaSpeed, col);
    }

    public void updateAndRender(int width, int height, int mouseX, int mouseY) {
        if (width != prevWidth || height != prevHeight || particles.isEmpty()) {
            init(width, height);
        }

        long now = System.currentTimeMillis();
        float delta = (lastTime == 0) ? 1.0F : Math.min(3.0F, (now - lastTime) / 16.666F);
        lastTime = now;

        Theme theme = Theme.getInstance();
        int accentColor = theme.getColor(0);
        float accR = ((accentColor >> 16) & 255) / 255.0F;
        float accG = ((accentColor >> 8) & 255) / 255.0F;
        float accB = (accentColor & 255) / 255.0F;

        // 1. Update particles & mouse repulsion
        for (Particle p : particles) {
            p.alphaPhase += p.alphaSpeed * delta;

            // Mouse interaction (gentle push/repel)
            float dx = p.x - mouseX;
            float dy = p.y - mouseY;
            float distSq = dx * dx + dy * dy;
            if (distSq < MOUSE_CONNECT_DIST * MOUSE_CONNECT_DIST && distSq > 0.01F) {
                float dist = (float) Math.sqrt(distSq);
                float force = (1.0F - (dist / MOUSE_CONNECT_DIST)) * 0.55F;
                p.x += (dx / dist) * force * delta;
                p.y += (dy / dist) * force * delta;
            }

            p.x += p.vx * delta;
            p.y += p.vy * delta;

            // Wrap around borders seamlessly
            if (p.x < -10) p.x = width + 10;
            if (p.x > width + 10) p.x = -10;
            if (p.y < -10) p.y = height + 10;
            if (p.y > height + 10) p.y = -10;
        }

        // 2. Render Constellation Lines between neighboring particles
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        int size = particles.size();
        for (int i = 0; i < size; i++) {
            Particle p1 = particles.get(i);
            for (int j = i + 1; j < size; j++) {
                Particle p2 = particles.get(j);
                float dx = p1.x - p2.x;
                float dy = p1.y - p2.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist < MAX_CONNECT_DIST) {
                    float lineAlpha = (1.0F - (dist / MAX_CONNECT_DIST)) * 0.16F;
                    wr.pos(p1.x, p1.y, 0.0D).color(accR, accG, accB, lineAlpha).endVertex();
                    wr.pos(p2.x, p2.y, 0.0D).color(accR, accG, accB, lineAlpha).endVertex();
                }
            }

            // Line from particle to mouse cursor if near
            float mdx = p1.x - mouseX;
            float mdy = p1.y - mouseY;
            float mdist = (float) Math.sqrt(mdx * mdx + mdy * mdy);
            if (mdist < MOUSE_CONNECT_DIST) {
                float mLineAlpha = (1.0F - (mdist / MOUSE_CONNECT_DIST)) * 0.28F;
                wr.pos(p1.x, p1.y, 0.0D).color(accR, accG, accB, mLineAlpha).endVertex();
                wr.pos(mouseX, mouseY, 0.0D).color(accR, accG, accB, mLineAlpha).endVertex();
            }
        }

        tessellator.draw();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        // 3. Render Particles with soft glow
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);

        for (Particle p : particles) {
            float pulse = (float) (Math.sin(p.alphaPhase) * 0.5 + 0.5);
            float currentAlpha = p.baseAlpha * (0.65F + pulse * 0.35F);

            int pR = (p.color >> 16) & 255;
            int pG = (p.color >> 8) & 255;
            int pB = p.color & 255;

            // Outer soft halo
            int haloAlpha = (int) (currentAlpha * 60.0F);
            int haloCol = (haloAlpha << 24) | (pR << 16) | (pG << 8) | pB;
            RenderUtil.drawCircle(p.x, p.y, p.radius * 2.2F, haloCol);

            // Core bright point
            int coreAlpha = (int) (currentAlpha * 240.0F);
            int coreCol = (coreAlpha << 24) | (pR << 16) | (pG << 8) | pB;
            RenderUtil.drawCircle(p.x, p.y, p.radius, coreCol);
        }

        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }
}

package coolware.util;

import coolware.module.visuals.Theme;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParticleRenderer {

    public static final List<Particle> particles = new ArrayList<Particle>();
    public static long lastTime = System.currentTimeMillis();
    private static boolean sentParticles;

    public static void renderParticle(EntityLivingBase target, float x, float y) {
        if (target == null) return;

        for (Particle p : particles) {
            GlStateManager.color(1, 1, 1, 1);
            if (p.opacity > 4) {
                p.render2D();
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastTime >= (1000 / 60)) {
            Iterator<Particle> iterator = particles.iterator();
            while (iterator.hasNext()) {
                Particle p = iterator.next();
                p.updatePosition();
                if (p.opacity < 1) {
                    iterator.remove();
                }
            }
            lastTime = now;
        }

        if (target.hurtTime == 9 && !sentParticles) {
            Theme theme = Theme.getInstance();
            for (int i = 0; i <= 10; i++) {
                Particle particle = new Particle();
                int c = (theme != null) ? (i % 2 == 0 ? theme.getColor(i * 100) : theme.getColor(-i * 100)) : 0xFFFF5555;
                particle.init(x + 20, y + 20,
                        (float) (((Math.random() - 0.5) * 2) * 1.4),
                        (float) (((Math.random() - 0.5) * 2) * 1.4),
                        (float) (4.0 + Math.random()),
                        c);
                particles.add(particle);
            }
            sentParticles = true;
        }
        if (target.hurtTime == 8) {
            sentParticles = false;
        }
    }
}

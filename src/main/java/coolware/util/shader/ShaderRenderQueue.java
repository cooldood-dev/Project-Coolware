package coolware.util.shader;

import coolware.util.shader.base.RiseShader;
import coolware.util.shader.base.ShaderRenderType;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class ShaderRenderQueue {
    private final List<Runnable> queue = new ArrayList<Runnable>();
    private RiseShader shader = null;

    public ShaderRenderQueue(RiseShader shader) {
        this.shader = shader;
    }

    public ShaderRenderQueue() {
    }

    public void render(ShaderRenderType renderType) {
        if (!this.queue.isEmpty()) {
            if (this.shader == null) {
                Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
                for (Runnable r : this.queue) {
                    r.run();
                }
            } else {
                this.shader.render(renderType, 0.0F, this.queue);
                if (renderType == ShaderRenderType.OVERLAY) {
                    this.shader.update();
                }
            }
        }
    }

    public void clear() {
        this.queue.clear();
    }

    public void add(Runnable runnable) {
        this.queue.add(runnable);
    }

    public List<Runnable> getQueue() {
        return this.queue;
    }

    public RiseShader getShader() {
        return this.shader;
    }
}

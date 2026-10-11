package coolware.util.shader;

import coolware.util.shader.base.ShaderRenderType;
import coolware.util.shader.impl.BloomShader;
import net.minecraft.client.renderer.GlStateManager;

import java.util.LinkedHashMap;
import java.util.Map;

public class ShaderRenderManager {
    private final Map<ShaderQueueType, ShaderRenderQueue> queues = new LinkedHashMap<ShaderQueueType, ShaderRenderQueue>();

    public ShaderRenderManager() {
        queues.put(ShaderQueueType.BLOOM, new ShaderRenderQueue(new BloomShader()));
        queues.put(ShaderQueueType.REGULAR, new ShaderRenderQueue(null));
    }

    public ShaderRenderQueue getQueue(ShaderQueueType type) {
        return queues.get(type);
    }

    public void render(ShaderRenderType renderType) {
        try {
            for (ShaderRenderQueue queue : queues.values()) {
                if (queue != null) {
                    queue.render(renderType);
                }
            }
        } finally {
            for (ShaderRenderQueue queue : queues.values()) {
                if (queue != null) {
                    queue.clear();
                }
            }
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
    }
}

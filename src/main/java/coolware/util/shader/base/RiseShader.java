package coolware.util.shader.base;

import java.util.List;

public abstract class RiseShader {
    private boolean active;

    public RiseShader() {
    }

    public abstract void render(ShaderRenderType renderType, float partialTicks, List<Runnable> runnables);

    public abstract void update();

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

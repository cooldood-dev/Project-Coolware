package coolware.util;

import java.awt.Color;

public class Particle {

    public float x, y, adjustedX, adjustedY, deltaX, deltaY, size;
    public int opacity;
    public Color color;

    public void render2D() {
        RenderUtil.drawGoodCircle(x + adjustedX + size / 2, y + adjustedY + size / 2, size / 2, ColorUtils.applyOpacity(color, opacity / 255.0F).getRGB());
    }

    public void updatePosition() {
        for (int i = 1; i <= 2; i++) {
            adjustedX += deltaX;
            adjustedY += deltaY;
            deltaY *= 0.97F;
            deltaX *= 0.97F;
            opacity -= 1;
            if (opacity < 1) opacity = 1;
        }
    }

    public void init(float x, float y, float deltaX, float deltaY, float size, int color) {
        this.x = x;
        this.y = y;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.size = size;
        this.opacity = 254;
        this.color = new Color(color, true);
    }
}

package coolware.module.visuals.esp;

import java.awt.Color;

public class ESPColor {
    private Color normalColor;
    private Color hurtColor;
    private Color targetColor;

    public ESPColor(Color normalColor, Color hurtColor, Color targetColor) {
        this.normalColor = normalColor;
        this.hurtColor = hurtColor;
        this.targetColor = targetColor;
    }

    public Color getColor() {
        return this.normalColor;
    }

    public Color getHurtColor() {
        return this.hurtColor;
    }

    public Color getTargetColor() {
        return this.targetColor;
    }

    public void setNormalColor(Color normalColor) {
        this.normalColor = normalColor;
    }

    public void setHurtColor(Color hurtColor) {
        this.hurtColor = hurtColor;
    }

    public void setTargetColor(Color targetColor) {
        this.targetColor = targetColor;
    }
}

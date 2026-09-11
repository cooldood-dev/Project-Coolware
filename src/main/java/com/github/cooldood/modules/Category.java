package com.github.cooldood.modules;

import com.github.cooldood.utils.render.EasingUtil;

import java.awt.*;

public enum Category {
    COMBAT(new Color(0x888888)),
    RENDER(new Color(0x888888)),
    MOVEMENT(new Color(0x888888)),
    PLAYER(new Color(0x888888)),
    CLIENT(new Color(0x888888));

    Category(Color color) {
        this.color = color;

        this.posX = 0; this.posY = 0;
        this.renderX = 0; this.renderY = 0;
    }

    public boolean shouldShow() {
        return (this.open || EasingUtil.getAnimation(this.name()) != -1) && !ModuleManager.getModulesByCategory(this).isEmpty();
    }

    public final Color color;

    public float posX, posY;
    public float renderX, renderY;

    public float scroll, renderScroll;

    public boolean open = true;
}

package com.github.cooldood.modules;

import com.github.cooldood.utils.render.EasingUtil;

import java.awt.*;

public enum Category {
    COMBAT(new Color(0x888888), com.github.cooldood.utils.render.IconFont.SWORD),
    RENDER(new Color(0x888888), com.github.cooldood.utils.render.IconFont.EYE),
    MOVEMENT(new Color(0x888888), com.github.cooldood.utils.render.IconFont.RUNNING),
    PLAYER(new Color(0x888888), com.github.cooldood.utils.render.IconFont.USER),
    CLIENT(new Color(0x888888), com.github.cooldood.utils.render.IconFont.COG);

    Category(Color color, String icon) {
        this.color = color;
        this.icon = icon;

        this.posX = 0; this.posY = 0;
        this.renderX = 0; this.renderY = 0;
    }

    public boolean shouldShow() {
        return (this.open || EasingUtil.getAnimation(this.name()) != -1) && !ModuleManager.getModulesByCategory(this).isEmpty();
    }

    public final Color color;
    public final String icon;

    public float posX, posY;
    public float renderX, renderY;

    public float scroll, renderScroll;

    public boolean open = true;
}

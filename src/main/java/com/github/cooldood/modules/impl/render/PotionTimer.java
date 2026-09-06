package com.github.cooldood.modules.impl.render;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;
import com.github.cooldood.utils.render.draggable.Draggable;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@RegisterModule(
        name = "Potion Timer",
        description = "Shows active potion effects with duration and timers.",
        category = Category.RENDER
)
public class PotionTimer extends Module {

    @RegisterSubModule(name = "Font Size", min = 6, max = 16, increment = 1)
    public static int fontSize = 9;

    @RegisterSubModule(name = "Show Progress Bar", description = "Show a duration progress bar under each potion")
    public static boolean showProgressBar = true;

    private static final float CORNER_RAD = 4.5f;
    private static final float PAD_X = 6f;
    private static final float PAD_Y = 5f;
    private static final float ROW_H = 18f;
    private static final float MIN_WIDTH = 100f;

    public static Draggable potionTimerDraggable = new Draggable(
            "PotionTimerHUD",
            () -> {
                if (C.p() == null) return new double[]{MIN_WIDTH, 20};

                Collection<PotionEffect> effects = C.p().getActivePotionEffects();
                if (effects.isEmpty()) {
                    if (C.mc.currentScreen instanceof com.github.cooldood.screens.ClickGUI.ClickGUIScreen) {
                        // In ClickGUI preview mode, draw placeholder
                        Color accent = ThemeModule.getAccentColor();
                        Color bg = ThemeModule.panelBackground;
                        RenderUtil.drawRoundedRect(0, 0, MIN_WIDTH, 22, CORNER_RAD, bg);
                        RenderUtil.drawRoundedRect(2, 2, 2, 18, 1, accent);
                        FontUtil.drawString("Potion Timer", 8, 7, fontSize, Color.WHITE, false);
                        return new double[]{MIN_WIDTH, 22};
                    }
                    return new double[]{0, 0};
                }

                List<PotionEffect> effectList = new ArrayList<>(effects);
                effectList.sort(Comparator.comparingInt(PotionEffect::getDuration).reversed());

                Color accent = ThemeModule.getAccentColor();
                Color panelBg = ThemeModule.panelBackground;
                Color textGray = new Color(175, 175, 185, 255);

                float fontH = FontUtil.getFontHeight(fontSize);
                float maxRowW = MIN_WIDTH;

                // Calculate required width
                for (PotionEffect effect : effectList) {
                    Potion potion = Potion.potionTypes[effect.getPotionID()];
                    if (potion == null) continue;

                    String name = I18n.format(potion.getName());
                    if (effect.getAmplifier() > 0) {
                        name += " " + (effect.getAmplifier() + 1);
                    }
                    String durationStr = Potion.getDurationString(effect);

                    float nameW = FontUtil.getStringWidth(name, fontSize);
                    float durW = FontUtil.getStringWidth(durationStr, fontSize - 1);
                    float totalW = PAD_X + 6 + nameW + 12 + durW + PAD_X;
                    if (totalW > maxRowW) maxRowW = totalW;
                }

                float curY = 0;

                for (PotionEffect effect : effectList) {
                    Potion potion = Potion.potionTypes[effect.getPotionID()];
                    if (potion == null) continue;

                    String name = I18n.format(potion.getName());
                    if (effect.getAmplifier() > 0) {
                        name += " " + (effect.getAmplifier() + 1);
                    }
                    String durationStr = Potion.getDurationString(effect);

                    // Liquid potion color
                    int liquidColor = potion.getLiquidColor();
                    Color effectColor = new Color(liquidColor);

                    // Background panel
                    RenderUtil.drawRoundedRect(0, curY, maxRowW, ROW_H, CORNER_RAD, panelBg);

                    // Left color accent bar
                    RenderUtil.drawRoundedRect(2, curY + 2, 2.5f, ROW_H - 4, 1, effectColor);

                    // Potion name
                    FontUtil.drawString(name, PAD_X + 4, curY + 3, fontSize, Color.WHITE, false);

                    // Duration string aligned right
                    float durW = FontUtil.getStringWidth(durationStr, fontSize - 1);
                    float durX = maxRowW - PAD_X - durW;
                    FontUtil.drawString(durationStr, durX, curY + 4, fontSize - 1, textGray, false);

                    // Progress bar
                    if (showProgressBar) {
                        // Rough max duration estimation based on potion tier or initial duration
                        float progress = Math.min(1.0f, (float) effect.getDuration() / 1200f);
                        float barW = (maxRowW - PAD_X * 2 - 4) * progress;
                        if (barW > 0) {
                            RenderUtil.drawRoundedRect(PAD_X + 2, curY + ROW_H - 2.5f, barW, 1.5f, 0.5f, accent);
                        }
                    }

                    curY += ROW_H + 3f;
                }

                return new double[]{maxRowW, curY};
            },
            e -> ModuleManager.isEnabled(PotionTimer.class),
            e -> true
    );

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    static {
        potionTimerDraggable.x = 0.01;
        potionTimerDraggable.y = 0.35;
    }
}

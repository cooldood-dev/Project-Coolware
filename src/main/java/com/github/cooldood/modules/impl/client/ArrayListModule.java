package com.github.cooldood.modules.impl.client;

import com.github.cooldood.modules.*;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.RenderUtil;
import com.github.cooldood.utils.render.draggable.Draggable;
import com.github.cooldood.utils.tenacity.animations.impl.DecelerateAnimation;
import com.github.cooldood.utils.tenacity.animations.Direction;
import com.github.cooldood.utils.tenacity.render.ColorUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@RegisterModule(
        name = "Array List",
        description = "Shows enabled modules.",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class ArrayListModule extends Module {

    @RegisterSubModule(name = "Font Size", min = 5, max = 30, increment = 2)
    public static double fontSize = 20;

    private static final HashMap<Module, DecelerateAnimation> moduleAnimations = new HashMap<>();
    private static float animatedMaxWidth = 0;

    private static class RenderState {
        Module m;
        String text;
        float x;
        float y;
        float width;
        float height;
        float scale;
    }
    
    private static List<RenderState> cachedStates = new ArrayList<>();
    private static float cachedFinalWidth = 0;
    private static float cachedFinalHeight = 0;
    
    private static void updatePhysics() {
        int size = (int) fontSize;
        List<Module> activeModules = new ArrayList<>(ModuleManager.getModules());
        activeModules.sort(Comparator.comparingDouble(m -> -FontUtil.getStringWidth(m.getAnnotation().name() + (!m.arrayListExtraInfo().isEmpty() ? " " + m.arrayListExtraInfo() : ""), size)));

        float y = 0;
        float targetMaxWidth = 0;

        for (Module m : activeModules) {
            DecelerateAnimation anim = moduleAnimations.computeIfAbsent(m, k -> new DecelerateAnimation(250, 1));
            anim.setDirection(m.isEnabled() && !m.hide ? Direction.FORWARDS : Direction.BACKWARDS);
            float scale = (float) anim.getOutput().floatValue();
            if (scale > 0.01f) {
                String text = m.getAnnotation().name() + (!m.arrayListExtraInfo().isEmpty() ? " \u00a77" + m.arrayListExtraInfo() : "");
                float width = FontUtil.getStringWidth(text, size);
                if (width > targetMaxWidth) targetMaxWidth = width;
            }
        }
        
        float diff = targetMaxWidth - animatedMaxWidth;
        animatedMaxWidth += diff * 0.1f;
        if (Math.abs(diff) < 0.1f) animatedMaxWidth = targetMaxWidth;
        
        float maxWidth = animatedMaxWidth;
        
        cachedStates.clear();
        for (Module m : activeModules) {
            DecelerateAnimation anim = moduleAnimations.get(m);
            float scale = (float) anim.getOutput().floatValue();
            if (scale <= 0.01f) continue;

            String text = m.getAnnotation().name() + (!m.arrayListExtraInfo().isEmpty() ? " \u00a77" + m.arrayListExtraInfo() : "");
            float width = FontUtil.getStringWidth(text, size);
            float height = FontUtil.getFontHeight(size) + 1;
            
            float x = maxWidth - (width * scale);
            
            RenderState state = new RenderState();
            state.m = m;
            state.text = text;
            state.x = x;
            state.y = y;
            state.width = width;
            state.height = height;
            state.scale = scale;
            cachedStates.add(state);
            
            y += height * scale;
        }
        cachedFinalWidth = maxWidth + 4;
        cachedFinalHeight = y;
    }

    public static Draggable arraylistDraggable = new Draggable(
            "ArrayListWidget2",
            () -> {
                boolean isBloom = com.github.cooldood.utils.render.draggable.DraggableRenderer.isBloom;
                boolean postProcEnabled = ModuleManager.isEnabled(com.github.cooldood.modules.impl.client.PostProcessing.class);
                
                // Only step animations ONCE per frame (in bloom pass, or normal pass if bloom is off)
                if (isBloom || !postProcEnabled) {
                    updatePhysics();
                }

                Color[] theme = ThemeModule.getThemeColours();
                int size = (int) fontSize;
                
                int index = 0;
                for (RenderState state : cachedStates) {
                    // Only draw background in normal pass
                    if (!isBloom) {
                        RenderUtil.drawRect(state.x - 2, state.y, state.width + 4, state.height, new Color(0, 0, 0, 120));
                    }
                    
                    Color c1 = ColorUtil.interpolateColorsBackAndForth(15, index * 20, theme[0], theme[theme.length > 1 ? 1 : 0], false);
                    
                    // The user requested NO BOLD. So we only draw it once!
                    // Also we don't draw drop shadows (!isBloom ensures no shadow in bloom pass, but since we disabled bold, we'll just use false for shadow everywhere if it's cleaner, wait, let's use shadow in normal pass, no shadow in bloom)
                    FontUtil.drawString(state.text, state.x, state.y + 1, size, c1, !isBloom);
                    
                    index++;
                }

                return new double[]{cachedFinalWidth, cachedFinalHeight};
            },
            e -> ModuleManager.isEnabled(ArrayListModule.class),
            e -> true
    );

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}
    static { arraylistDraggable.anchor = Draggable.Anchor.RIGHT; arraylistDraggable.x = 0.99; arraylistDraggable.y = 0.01; }
}

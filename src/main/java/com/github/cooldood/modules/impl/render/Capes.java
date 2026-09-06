package com.github.cooldood.modules.impl.render;

import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.modules.impl.client.ThemeModule;
import com.github.cooldood.utils.client.C;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.awt.image.BufferedImage;

@RegisterModule(
        name = "Capes",
        description = "Renders a custom client-side Coolware cape on the player.",
        category = Category.RENDER,
        enabledByDefault = true
)
public class Capes extends Module {

    @RegisterSubModule(name = "Cape Style")
    public static CapeStyle style = CapeStyle.Coolware;
    public enum CapeStyle {
        Coolware, Minimal, Dark
    }

    private static ResourceLocation capeResourceLocation;
    private static Color lastBakedColor;
    private static CapeStyle lastBakedStyle;

    public static ResourceLocation getCapeResource() {
        Color currentAccent = ThemeModule.getAccentColor();
        if (capeResourceLocation == null || !currentAccent.equals(lastBakedColor) || style != lastBakedStyle) {
            bakeCapeTexture(currentAccent, style);
        }
        return capeResourceLocation;
    }

    private static void bakeCapeTexture(Color accent, CapeStyle capeStyle) {
        lastBakedColor = accent;
        lastBakedStyle = capeStyle;

        int width = 64;
        int height = 32;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Cape texture layout in Minecraft standard:
        // Top front: (1, 1, 10, 16)
        // Top back (the visible side when running): (12, 1, 10, 16)
        
        // Background color
        Color bg = capeStyle == CapeStyle.Dark ? new Color(12, 12, 16) : new Color(20, 20, 26);
        g.setColor(bg);
        g.fillRect(0, 0, width, height);

        // Fill back area (12, 1, 10, 16)
        // Scale coordinate mapping: standard texture is 64x32
        // Visible outer back face is at x=12..22, y=1..17
        g.setColor(bg);
        g.fillRect(12, 1, 10, 16);

        // Accent top and bottom borders on back face
        g.setColor(accent);
        g.fillRect(12, 1, 10, 1);
        g.fillRect(12, 16, 10, 1);
        g.fillRect(12, 1, 1, 16);
        g.fillRect(21, 1, 1, 16);

        // Center "C" logo or Coolware badge on back face
        g.setColor(accent);
        // Draw stylized "C" in center (around x=15..18, y=7..11)
        g.fillRect(14, 5, 4, 1);  // top bar
        g.fillRect(14, 5, 1, 7);  // left spine
        g.fillRect(14, 11, 4, 1); // bottom bar
        g.fillRect(17, 8, 1, 4);  // inner tab

        // Subtle gradient highlight
        g.setColor(new Color(255, 255, 255, 40));
        g.fillRect(13, 2, 8, 2);

        // Front face (1, 1, 10, 16)
        g.setColor(bg);
        g.fillRect(1, 1, 10, 16);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
        g.fillRect(1, 1, 10, 1);
        g.fillRect(1, 16, 10, 1);

        g.dispose();

        DynamicTexture dynamicTexture = new DynamicTexture(image);
        capeResourceLocation = C.mc.getTextureManager().getDynamicTextureLocation("coolware_cape", dynamicTexture);
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}
}

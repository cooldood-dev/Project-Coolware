package coolware.util.render.font;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;

public class AmbientFonts {
    private static final HashMap<Integer, TTFFontRenderer> NUNITO = new HashMap<Integer, TTFFontRenderer>();
    private static final HashMap<Integer, TTFFontRenderer> OPEN_SANS_REGULAR = new HashMap<Integer, TTFFontRenderer>();
    private static final HashMap<Integer, TTFFontRenderer> SAN_FRANCISCO = new HashMap<Integer, TTFFontRenderer>();

    public static TTFFontRenderer getNunito(final int size) {
        return get(NUNITO, size, "Nunito", true, true);
    }

    public static TTFFontRenderer getOpenSansRegular(final int size) {
        return get(OPEN_SANS_REGULAR, size, "OpenSans-Regular", true, true);
    }

    public static TTFFontRenderer getSanFrancisco(final int size) {
        return get(SAN_FRANCISCO, size, "SF-Pro", true, true);
    }

    private static TTFFontRenderer get(HashMap<Integer, TTFFontRenderer> map, int size, String name, boolean antialiasing, boolean fractionalMetrics) {
        if (!map.containsKey(size)) {
            Font font = null;
            InputStream in = null;
            try {
                try {
                    in = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("dogclient/font/" + name + ".ttf")).getInputStream();
                } catch (Throwable ignored) {
                    in = AmbientFonts.class.getResourceAsStream("/assets/minecraft/dogclient/font/" + name + ".ttf");
                }
                if (in != null) {
                    font = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont((float) size);
                }
            } catch (final Throwable ignored) {
            } finally {
                if (in != null) {
                    try { in.close(); } catch (Throwable ignored) {}
                }
            }

            if (font == null) {
                font = new Font("Arial", Font.PLAIN, size);
            }

            map.put(size, new TTFFontRenderer(font, antialiasing, fractionalMetrics));
        }

        return map.get(size);
    }
}
package coolware.font.moonlight;

import java.awt.Font;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public enum Fonts {
    interBold("inter/Inter_Bold"),
    interMedium("inter/Inter_Medium"),
    interRegular("inter/Inter_Regular"),
    interSemiBold("inter/Inter_SemiBold"),
    psRegular("product-sans/Regular"),
    psBold("product-sans/Bold"),
    nursultan("others/Nursultan"),
    Tahoma("others/Exhi"),
    skeet("others/skeet"),
    noti("others/noti"),
    noti2("others/noti2"),
    session("others/session"),
    session2("others/session2"),
    neverlose("others/nlicon"),
    sfui("others/sfui"),
    nunito("others/Nunito"),
    openSansRegular("others/OpenSans-Regular");

    private final String file;
    private final Map<Float, FontRenderer> fontMap = new HashMap<Float, FontRenderer>();

    Fonts(String file) {
        this.file = file;
    }

    public FontRenderer get(float size) {
        return get(size, true);
    }

    public FontRenderer get(float size, boolean antiAlias) {
        FontRenderer fr = fontMap.get(size);
        if (fr == null) {
            fr = create(this.file, size, antiAlias);
            fontMap.put(size, fr);
        }
        return fr;
    }

    public static FontRenderer create(String file, float size, boolean antiAlias) {
        InputStream in = null;
        try {
            in = Fonts.class.getResourceAsStream("/assets/coolware/font/" + file + ".ttf");
            if (in == null) {
                in = Fonts.class.getResourceAsStream("/assets/minecraft/moonlight/font/" + file + ".ttf");
            }
            if (in == null) {
                in = Fonts.class.getClassLoader().getResourceAsStream("assets/coolware/font/" + file + ".ttf");
            }
            if (in == null) {
                in = Fonts.class.getClassLoader().getResourceAsStream("assets/minecraft/moonlight/font/" + file + ".ttf");
            }

            if (in != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(Font.PLAIN, size);
                return new FontRenderer(font, antiAlias);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {}
            }
        }

        // Fallback to system font if loading stream fails
        Font fallback = new Font("Arial", Font.PLAIN, (int) size);
        return new FontRenderer(fallback, antiAlias);
    }
}

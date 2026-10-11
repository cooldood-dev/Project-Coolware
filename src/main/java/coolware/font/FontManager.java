package coolware.font;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static final Map<String, Font> fontMap = new HashMap<>();

    public static CFontRenderer sfRegular12;
    public static CFontRenderer sfRegular14;
    public static CFontRenderer sfRegular16;
    public static CFontRenderer sfRegular18;

    public static CFontRenderer sfMedium14;
    public static CFontRenderer sfMedium16;
    public static CFontRenderer sfMedium18;
    public static CFontRenderer sfMedium20;

    public static CFontRenderer sfSemiBold14;
    public static CFontRenderer sfSemiBold16;
    public static CFontRenderer sfSemiBold18;
    public static CFontRenderer sfSemiBold20;

    public static CFontRenderer sfBold14;
    public static CFontRenderer sfBold16;
    public static CFontRenderer sfBold18;
    public static CFontRenderer sfBold22;
    public static CFontRenderer sfBold28;
    public static CFontRenderer sfBold36;
    public static CFontRenderer sfBold48;

    // Font Awesome 5 Free Regular Icon Renderers
    public static CFontRenderer faRegular14;
    public static CFontRenderer faRegular16;
    public static CFontRenderer faRegular18;
    public static CFontRenderer faRegular20;
    public static CFontRenderer faRegular22;
    public static CFontRenderer faRegular24;
    public static CFontRenderer faRegular28;

    public static void init() {
        try {
            Font regular = getFont("sf-regular.otf", 18);
            Font bold = getFont("sf-bold.ttf", 18);
            Font medium = getFont("sf-medium.otf", 18);
            Font semiBold = getFont("sf-semibold.otf", 18);
            Font faRegular = getFontAwesome(18);

            sfRegular12 = new CFontRenderer(regular.deriveFont(Font.PLAIN, 12), true, true);
            sfRegular14 = new CFontRenderer(regular.deriveFont(Font.PLAIN, 14), true, true);
            sfRegular16 = new CFontRenderer(regular.deriveFont(Font.PLAIN, 16), true, true);
            sfRegular18 = new CFontRenderer(regular.deriveFont(Font.PLAIN, 18), true, true);

            sfMedium14 = new CFontRenderer(medium.deriveFont(Font.PLAIN, 14), true, true);
            sfMedium16 = new CFontRenderer(medium.deriveFont(Font.PLAIN, 16), true, true);
            sfMedium18 = new CFontRenderer(medium.deriveFont(Font.PLAIN, 18), true, true);
            sfMedium20 = new CFontRenderer(medium.deriveFont(Font.PLAIN, 20), true, true);

            sfSemiBold14 = new CFontRenderer(semiBold.deriveFont(Font.PLAIN, 14), true, true);
            sfSemiBold16 = new CFontRenderer(semiBold.deriveFont(Font.PLAIN, 16), true, true);
            sfSemiBold18 = new CFontRenderer(semiBold.deriveFont(Font.PLAIN, 18), true, true);
            sfSemiBold20 = new CFontRenderer(semiBold.deriveFont(Font.PLAIN, 20), true, true);

            sfBold14 = new CFontRenderer(bold.deriveFont(Font.PLAIN, 14), true, true);
            sfBold16 = new CFontRenderer(bold.deriveFont(Font.PLAIN, 16), true, true);
            sfBold18 = new CFontRenderer(bold.deriveFont(Font.PLAIN, 18), true, true);
            sfBold22 = new CFontRenderer(bold.deriveFont(Font.PLAIN, 22), true, true);
            sfBold28 = new CFontRenderer(bold.deriveFont(Font.PLAIN, 28), true, true);
            sfBold36 = new CFontRenderer(bold.deriveFont(Font.PLAIN, 36), true, true);
            sfBold48 = new CFontRenderer(bold.deriveFont(Font.PLAIN, 48), true, true);

            if (faRegular != null) {
                faRegular14 = new CFontRenderer(faRegular.deriveFont(Font.PLAIN, 14), true, true);
                faRegular16 = new CFontRenderer(faRegular.deriveFont(Font.PLAIN, 16), true, true);
                faRegular18 = new CFontRenderer(faRegular.deriveFont(Font.PLAIN, 18), true, true);
                faRegular20 = new CFontRenderer(faRegular.deriveFont(Font.PLAIN, 20), true, true);
                faRegular22 = new CFontRenderer(faRegular.deriveFont(Font.PLAIN, 22), true, true);
                faRegular24 = new CFontRenderer(faRegular.deriveFont(Font.PLAIN, 24), true, true);
                faRegular28 = new CFontRenderer(faRegular.deriveFont(Font.PLAIN, 28), true, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to system font if loading stream fails
            Font sysFont = new Font("Arial", Font.BOLD, 18);
            sfRegular12 = new CFontRenderer(sysFont.deriveFont(12f), true, true);
            sfRegular14 = new CFontRenderer(sysFont.deriveFont(14f), true, true);
            sfRegular16 = new CFontRenderer(sysFont.deriveFont(16f), true, true);
            sfRegular18 = new CFontRenderer(sysFont, true, true);

            sfMedium14 = sfRegular14;
            sfMedium16 = sfRegular16;
            sfMedium18 = sfRegular18;
            sfMedium20 = new CFontRenderer(sysFont.deriveFont(20f), true, true);

            sfSemiBold14 = new CFontRenderer(sysFont.deriveFont(14f), true, true);
            sfSemiBold16 = new CFontRenderer(sysFont.deriveFont(16f), true, true);
            sfSemiBold18 = new CFontRenderer(sysFont.deriveFont(18f), true, true);
            sfSemiBold20 = new CFontRenderer(sysFont.deriveFont(20f), true, true);

            sfBold14 = new CFontRenderer(sysFont.deriveFont(14f), true, true);
            sfBold16 = new CFontRenderer(sysFont.deriveFont(16f), true, true);
            sfBold18 = new CFontRenderer(sysFont.deriveFont(18f), true, true);
            sfBold22 = new CFontRenderer(sysFont.deriveFont(22f), true, true);
            sfBold28 = new CFontRenderer(sysFont.deriveFont(28f), true, true);
            sfBold36 = new CFontRenderer(sysFont.deriveFont(36f), true, true);
            sfBold48 = new CFontRenderer(sysFont.deriveFont(48f), true, true);
        }
    }

    private static Font getFont(String fontName, int size) {
        if (fontMap.containsKey(fontName)) {
            return fontMap.get(fontName).deriveFont((float) size);
        }

        try {
            InputStream is = FontManager.class.getResourceAsStream("/assets/coolware/fonts/" + fontName);
            if (is == null) {
                is = FontManager.class.getClassLoader().getResourceAsStream("assets/coolware/fonts/" + fontName);
            }
            if (is != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                fontMap.put(fontName, font);
                return font.deriveFont((float) size);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Font("Arial", Font.BOLD, size);
    }

    private static Font getFontAwesome(int size) {
        if (fontMap.containsKey("fontawesome")) {
            return fontMap.get("fontawesome").deriveFont((float) size);
        }

        String[] paths = new String[]{
                "/assets/coolware/fonts/fa-regular.ttf",
                "/assets/coolware/fonts/fa-regular-400.ttf",
                "assets/coolware/fonts/fa-regular.ttf",
                "assets/coolware/fonts/fa-regular-400.ttf"
        };

        for (String path : paths) {
            try {
                InputStream is = FontManager.class.getResourceAsStream(path);
                if (is == null) {
                    is = FontManager.class.getClassLoader().getResourceAsStream(path);
                }
                if (is != null) {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                    fontMap.put("fontawesome", font);
                    return font.deriveFont((float) size);
                }
            } catch (Exception ignored) {
            }
        }

        // Direct filesystem fallback
        File localFile = new File("C:/Users/Admin/Documents/CoolwareV2.01/font-awesome-5-free-regular/Font Awesome 5 Free Regular/Font Awesome 5 Free Regular.ttf");
        if (localFile.exists()) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, localFile);
                fontMap.put("fontawesome", font);
                return font.deriveFont((float) size);
            } catch (Exception ignored) {
            }
        }

        return new Font("Dialog", Font.PLAIN, size);
    }
}

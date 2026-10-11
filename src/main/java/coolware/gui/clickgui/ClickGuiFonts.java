package coolware.gui.clickgui;

import coolware.Coolware;
import coolware.font.CFontRenderer;
import coolware.font.FontManager;
import coolware.module.visuals.ClickGUI;

import java.util.HashMap;
import java.util.Map;

final class ClickGuiFonts {
    private static final Map<String, CFontRenderer> cachedFonts = new HashMap<String, CFontRenderer>();

    private ClickGuiFonts() {}

    static CFontRenderer regular() {
        int base = Math.max(10, Math.min(18, getSize()));
        return scaled(FontManager.sfRegular14 != null ? FontManager.sfRegular14 : FontManager.sfBold18, base);
    }

    static CFontRenderer bold() {
        int base = Math.max(11, Math.min(18, getSize() + 1));
        return scaled(FontManager.sfBold18, base);
    }

    static CFontRenderer small() {
        int base = Math.max(9, Math.min(15, Math.round(getSize() * 0.85f)));
        return scaled(FontManager.sfRegular12 != null ? FontManager.sfRegular12 : (FontManager.sfRegular14 != null ? FontManager.sfRegular14 : FontManager.sfBold18), base);
    }

    private static int getSize() {
        ClickGUI clickGUI = Coolware.getInstance().getModuleManager().getModule(ClickGUI.class);
        return clickGUI == null ? 14 : clickGUI.getFontSize();
    }

    private static CFontRenderer scaled(CFontRenderer template, int size) {
        if (template == null) {
            return null;
        }

        String key = template.getFont().getName() + ':' + template.getFont().getStyle() + ':' + size;
        CFontRenderer font = cachedFonts.get(key);
        if (font == null) {
            font = new CFontRenderer(template.getFont().deriveFont((float) size), true, true);
            cachedFonts.put(key, font);
        }
        return font;
    }
}

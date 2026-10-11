package coolware.module.visuals;

import coolware.Coolware;
import coolware.gui.clickgui.ClickGuiScreen;
import coolware.gui.clickgui.novoline.NovolineDropdownGUI;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class ClickGUI extends Module {
    private final ModeSetting modeSetting = new ModeSetting("Style", this, "Novoline", "Novoline", "Cool");
    private final NumberSetting fontSizeSetting = new NumberSetting("Font Size", this, 14.0, 10.0, 22.0, 1.0);

    // Persistent GUI instances so panel positions are remembered between opens
    private ClickGuiScreen coolGuiScreen;

    public ClickGUI() {
        super("ClickGUI", "Displays the graphical module menu", Category.CLIENT, Keyboard.KEY_RSHIFT);
        addSetting(modeSetting);
        addSetting(fontSizeSetting);
    }

    public int getFontSize() {
        return fontSizeSetting.getValueInt();
    }

    /** Returns true when the "Cool" (classic card) mode is selected. */
    public boolean isCoolMode() {
        return "Cool".equalsIgnoreCase(modeSetting.getMode());
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null) {
            if (isCoolMode()) {
                // Classic rounded glassmorphic card GUI (from coolware-v2-main)
                if (coolGuiScreen == null) {
                    coolGuiScreen = new ClickGuiScreen();
                }
                mc.displayGuiScreen(coolGuiScreen);
            } else {
                // Default Novoline dropdown GUI
                mc.displayGuiScreen(Coolware.getInstance().getClickGuiScreen());
            }
        }
        setToggled(false);
    }
}

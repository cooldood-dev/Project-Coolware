package coolware.gui.clickgui.novoline;

import coolware.font.novoline.impl.Fonts;
import coolware.module.visuals.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.MathHelper;

import java.awt.Color;

import static coolware.font.novoline.impl.Fonts.SF.SF_18.SF_18;

public class NovolineConfigItem {

    public int y;
    public final static Color WHITE = new Color(255, 255, 255, 255);
    private final String name;
    private final NovolineConfigTab parent;
    private float fraction = 0;
    private final NovolineTimer lastClick = new NovolineTimer();

    public NovolineConfigItem(String name, NovolineConfigTab parent) {
        this.name = name;
        this.parent = parent;
    }

    public void drawScreen(int mouseX, int mouseY) {
        y = (int) (parent.getPosY() + 31);
        boolean isSelected = parent.getSelectedConfig() == this;
        int hudColor = Theme.getInstance().getColor(0);

        for (NovolineConfigItem config : parent.getConfigs()) {
            if (config == this) {
                break;
            } else {
                y += config.getYPerConfig();
            }
        }

        int fps = Math.max(Minecraft.getMinecraft().getDebugFPS(), 15);
        if (isSelected && fraction < 1) {
            fraction += 0.0025f * (2000.0f / fps);
        }
        if (!isSelected && fraction > 0) {
            fraction -= 0.0025f * (2000.0f / fps);
        }
        fraction = MathHelper.clamp_float(fraction, 0, 1);

        Gui.drawRect((int) parent.getPosX(), y, (int) parent.getPosX() + 100, y + getYPerConfig(), new Color(40, 40, 40, 255).getRGB());
        SF_18.drawCenteredString(name, parent.getPosX() + 101.0f / 2.0f, y + 4,
                NovolineRenderUtils.interpolateColor(WHITE, new Color(hudColor), fraction), true);
    }

    public void keyTyped(char typedChar, int keyCode) {}

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 0) {
            parent.setSelectedConfig(this);
            if (!lastClick.check(200)) {
                coolware.config.ConfigManager.loadConfig(getName());
            }
            lastClick.reset();
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        y = (int) (parent.getPosY() + 31);
        for (NovolineConfigItem tabModule : parent.getConfigs()) {
            if (tabModule == this) {
                break;
            } else {
                y += tabModule.getYPerConfig();
            }
        }
        return mouseX >= parent.getPosX() && mouseY >= y && mouseX <= parent.getPosX() + 101 && mouseY <= y + getYPerConfig();
    }

    public int getYPerConfig() {
        return 14;
    }

    public String getName() {
        return name;
    }

    public NovolineConfigTab getParent() {
        return parent;
    }
}

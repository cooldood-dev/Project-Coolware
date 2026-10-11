package coolware.gui.clickgui.novoline;

import coolware.font.novoline.impl.Fonts;
import coolware.module.visuals.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.MathHelper;

import java.awt.Color;
import java.util.function.Consumer;

public class NovolineConfigButton extends NovolineConfigItem {

    private final Consumer<String> actionPerformed;
    private float fraction = 0;

    public NovolineConfigButton(String name, NovolineConfigTab parent, Consumer<String> actionPerformed) {
        super(name, parent);
        this.actionPerformed = actionPerformed;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        int hudColor = Theme.getInstance().getColor(0);
        y = (int) (getParent().getPosY() + 31);

        for (NovolineConfigItem config : getParent().getConfigs()) {
            if (config == this) {
                break;
            } else {
                y += config.getYPerConfig();
            }
        }

        int fps = Math.max(Minecraft.getMinecraft().getDebugFPS(), 15);
        if (isHovered(mouseX, mouseY) && fraction < 1) {
            fraction += 0.0025f * (2000.0f / fps);
        } else if (fraction > 0) {
            fraction -= 0.0025f * (2000.0f / fps);
        }
        fraction = MathHelper.clamp_float(fraction, 0, 1);

        Gui.drawRect((int) getParent().getPosX(), y, (int) getParent().getPosX() + 100, y + getYPerConfig(), new Color(40, 40, 40, 255).getRGB());
        Fonts.SF.SF_19.SF_19.drawString(getName(), getParent().getPosX() + 2, y + 4,
                NovolineRenderUtils.interpolateColor(new Color(255, 255, 255, 255), new Color(hudColor), fraction));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 0) {
            if (getParent().getSelectedConfig() == null) {
                actionPerformed.accept("");
            } else {
                actionPerformed.accept(getParent().getSelectedConfig().getName());
            }
        }
    }
}

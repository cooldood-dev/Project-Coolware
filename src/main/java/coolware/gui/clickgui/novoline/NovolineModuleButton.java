package coolware.gui.clickgui.novoline;

import coolware.font.novoline.impl.Fonts;
import coolware.module.Module;
import coolware.module.visuals.Theme;
import coolware.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static coolware.font.novoline.impl.Fonts.SF.SF_18.SF_18;

public class NovolineModuleButton {

    public final Module module;
    public int yPerModule, y;
    public NovolineTab tab;
    public boolean opened;
    public List<NovolineSettingElement> settings = new CopyOnWriteArrayList<NovolineSettingElement>();
    public NovolineTimer hoverTimer = new NovolineTimer();

    private double length = 3, anim = 5;
    float fraction = 0;
    float fractionBackground = 0;

    public NovolineModuleButton(Module module, NovolineTab tab) {
        this.module = module;
        this.tab = tab;
        for (Setting setting : module.getSettings()) {
            settings.add(new NovolineSettingElement(setting, this));
        }
    }

    public void drawScreen(int mouseX, int mouseY) {
        Minecraft instance = Minecraft.getMinecraft();
        int debugFPS = Math.max(instance.getDebugFPS(), 15);

        if (module.isToggled() && fraction < 1) {
            fraction += 0.0025f * (2000.0f / debugFPS);
        }
        if (!module.isToggled() && fraction > 0) {
            fraction -= 0.0025f * (2000.0f / debugFPS);
        }

        if (!module.isToggled()) {
            if (isHovered(mouseX, mouseY) && fractionBackground < 1) {
                fractionBackground += 0.0025f * (2000.0f / debugFPS);
            }
            if (!isHovered(mouseX, mouseY) && fractionBackground > 0) {
                fractionBackground -= 0.0025f * (2000.0f / debugFPS);
            }
        }

        fraction = MathHelper.clamp_float(fraction, 0.0F, 1.0F);
        fractionBackground = MathHelper.clamp_float(fractionBackground, 0.0F, 1.0F);

        int targetY = getY();
        if (yPerModule < targetY) {
            yPerModule = (int) (yPerModule + (targetY + 9 - yPerModule) * 0.1);
        } else if (yPerModule > targetY) {
            yPerModule = (int) (yPerModule + (targetY - yPerModule) * 0.1);
        }

        y = (int) (tab.getPosY() + 15);
        for (NovolineModuleButton tabModule : tab.getModules()) {
            if (tabModule == this) {
                break;
            } else {
                y += tabModule.yPerModule;
            }
        }

        Color colorHUD = new Color(Theme.getInstance().getColor(0));

        Gui.drawRect((int) tab.getPosX(), y, (int) tab.getPosX() + 100, y + yPerModule, new Color(40, 40, 40, 255).getRGB());
        if (!module.isToggled() && fraction == 0) {
            Gui.drawRect((int) tab.getPosX(), y, (int) tab.getPosX() + 100, y + 14,
                    NovolineRenderUtils.interpolateColor(new Color(40, 40, 40, 255), new Color(29, 29, 29, 255), MathHelper.clamp_float(fractionBackground, 0, 1)));
        } else {
            Gui.drawRect((int) tab.getPosX(), y, (int) tab.getPosX() + 100, y + 14,
                    NovolineRenderUtils.interpolateColor(new Color(40, 40, 40, 255), colorHUD, MathHelper.clamp_float(fraction, 0, 1)));
        }

        SF_18.drawString(module.getName(), tab.getPosX() + 2, (float) (y + 4), 0xFFFFFFFF, true);

        if (hasVisibleSettings()) {
            double val = debugFPS / 8.3;
            if (opened && length > -3) {
                length -= 3.0 / val;
            } else if (!opened && length < 3) {
                length += 3.0 / val;
            }
            if (opened && anim < 8) {
                anim += 3.0 / val;
            } else if (!opened && anim > 5) {
                anim -= 3.0 / val;
            }
            NovolineRenderUtils.drawArrow(tab.getPosX() + 92, y + anim, 2, 0xFFFFFFFF, length);
        }

        if (opened || yPerModule != 14) {
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            if (yPerModule != getY() && scaledResolution.getScaleFactor() != 1) {
                GL11.glScissor(0,
                        scaledResolution.getScaledHeight() * scaledResolution.getScaleFactor() - y * scaledResolution.getScaleFactor() - yPerModule * scaledResolution.getScaleFactor(),
                        scaledResolution.getScaledWidth() * scaledResolution.getScaleFactor(),
                        yPerModule * scaledResolution.getScaleFactor());
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                for (NovolineSettingElement s : settings) {
                    if (s.setting.isVisible()) {
                        s.drawScreen(mouseX, mouseY);
                    }
                }
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            } else {
                for (NovolineSettingElement s : settings) {
                    if (s.setting.isVisible()) {
                        s.drawScreen(mouseX, mouseY);
                    }
                }
            }
        } else {
            for (NovolineSettingElement s : settings) {
                s.setPercent(0);
            }
        }
    }

    public boolean hasVisibleSettings() {
        for (NovolineSettingElement s : settings) {
            if (s.setting.isVisible()) return true;
        }
        return false;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (opened) {
            for (NovolineSettingElement s : settings) {
                if (s.setting.isVisible()) {
                    s.keyTyped(typedChar, keyCode);
                }
            }
        }
    }

    public int getY() {
        if (opened) {
            int i = 17;
            for (NovolineSettingElement s : settings) {
                if (s.setting.isVisible()) {
                    i += s.getHeight();
                }
            }
            return i;
        } else {
            return 14;
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isHovered(mouseX, mouseY)) {
            switch (mouseButton) {
                case 0:
                    module.toggle();
                    break;
                case 1:
                    if (hasVisibleSettings()) {
                        opened = !opened;
                    }
                    break;
            }
        }
        if (opened) {
            for (NovolineSettingElement s : settings) {
                if (s.setting.isVisible()) {
                    try {
                        s.mouseClicked(mouseX, mouseY, mouseButton);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (opened) {
            for (NovolineSettingElement s : settings) {
                if (s.setting.isVisible()) {
                    s.mouseReleased(mouseX, mouseY, state);
                }
            }
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        y = (int) (tab.getPosY() + 15);
        for (NovolineModuleButton tabModule : tab.getModules()) {
            if (tabModule == this) {
                break;
            } else {
                y += tabModule.yPerModule;
            }
        }
        if (opened) {
            return mouseX >= tab.getPosX() && mouseY >= y && mouseX <= tab.getPosX() + 101 && mouseY <= y + 14;
        }
        return mouseX >= tab.getPosX() && mouseY >= y && mouseX <= tab.getPosX() + 101 && mouseY <= y + yPerModule;
    }
}

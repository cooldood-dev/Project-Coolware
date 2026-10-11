package coolware.gui.clickgui.novoline;

import coolware.font.novoline.impl.Fonts;
import coolware.module.visuals.Theme;
import coolware.setting.BooleanSetting;
import coolware.setting.KeybindSetting;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;

import static coolware.font.novoline.impl.Fonts.SF.SF_17.SF_17;
import static coolware.font.novoline.impl.Fonts.SF.SF_18.SF_18;
import static coolware.font.novoline.impl.Fonts.SF.SF_26.SF_26;

public class NovolineSettingElement {

    public final Setting setting;
    private final NovolineModuleButton moduleButton;
    public boolean opened; // used for ModeSetting dropdown
    private boolean dragging;
    public float percent = 0;

    public NovolineSettingElement(Setting setting, NovolineModuleButton moduleButton) {
        this.setting = setting;
        this.moduleButton = moduleButton;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    public void drawScreen(int mouseX, int mouseY) {
        if (!setting.isVisible()) return;

        int y = getY();
        int hudColor = Theme.getInstance().getColor(0);
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        boolean scissor = scaledResolution.getScaleFactor() != 1;
        double clamp = MathHelper.clamp_double(Minecraft.getMinecraft().getDebugFPS() / 30.0, 1.0, 9999.0);

        if (setting instanceof NumberSetting) {
            NumberSetting numberSetting = (NumberSetting) setting;
            if (moduleButton.yPerModule == moduleButton.getY() && scissor) {
                GL11.glPushMatrix();
                GL11.glScissor((int) (moduleButton.tab.getPosX() * 2 + 1), 0, 197, 999999999);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }

            double rounded = Math.round(numberSetting.getValue() * 100.0) / 100.0;
            double percentBar = (numberSetting.getValue() - numberSetting.getMin()) / (numberSetting.getMax() - numberSetting.getMin());

            percent = Math.max(0, Math.min(1, (float) (percent + (Math.max(0, Math.min(percentBar, 1)) - percent) * (0.2 / clamp))));
            Gui.drawRect((int) moduleButton.tab.getPosX() + 1, y + 3, (int) moduleButton.tab.getPosX() + 99, y + 14, new Color(0, 0, 0, 50).getRGB());
            Gui.drawRect((int) moduleButton.tab.getPosX() + 1, y + 3, (int) (moduleButton.tab.getPosX() + 1 + 98 * percent), y + 14, hudColor);
            SF_18.drawString(setting.getName() + " " + rounded, moduleButton.tab.getPosX() + 4, y + 5.5f, 0xFFFFFFFF, true);

            if (this.dragging) {
                double difference = numberSetting.getMax() - numberSetting.getMin();
                double value = numberSetting.getMin() + MathHelper.clamp_double((mouseX - (moduleButton.tab.getPosX() + 1)) / 98.0, 0.0, 1.0) * difference;
                numberSetting.setValue(value);
            }

            if (moduleButton.yPerModule == moduleButton.getY() && scissor) {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GL11.glPopMatrix();
            }
        } else if (setting instanceof BooleanSetting) {
            BooleanSetting boolSetting = (BooleanSetting) setting;
            Gui.drawRect((int) moduleButton.tab.getPosX() + 89, y + 4, (int) moduleButton.tab.getPosX() + 99, y + 14, new Color(0, 0, 0, 50).getRGB());
            if (boolSetting.isEnabled()) {
                NovolineRenderUtils.drawCheck(moduleButton.tab.getPosX() + 91, y + 8.5f, 2, new Color(hudColor).brighter().getRGB());
            }

            SF_18.drawString(setting.getName(), moduleButton.tab.getPosX() + 4, y + 5.5f, new Color(227, 227, 227, 255).getRGB(), true);
        } else if (setting instanceof ModeSetting) {
            ModeSetting modeSetting = (ModeSetting) setting;
            if (opened) {
                // Expanded dropdown mode (Selectbox style)
                int h = modeSetting.getModes().size() * 17 + 2;
                Gui.drawRect((int) moduleButton.tab.getPosX() + 1, y + 3, (int) moduleButton.tab.getPosX() + 99, y + 14 + h, new Color(0, 0, 0, 50).getRGB());
                String displayName = modeSetting.getName();
                SF_18.drawString(displayName, moduleButton.tab.getPosX() + 49.5f - SF_18.stringWidth(displayName) / 2.0f, y + 5.5f, Color.WHITE.getRGB(), true);

                int yS = y + 17;
                for (String mode : modeSetting.getModes()) {
                    boolean isCurrent = modeSetting.is(mode);
                    int rgb = isCurrent ? 0xFFFFFFFF : new Color(174, 174, 174, 150).getRGB();
                    SF_18.drawString(mode, moduleButton.tab.getPosX() + 15, (float) (yS + 6), rgb, true);
                    SF_26.drawString(".", moduleButton.tab.getPosX() + 9, yS, rgb);
                    yS += 17;
                }
            } else {
                // Collapsed Combobox style
                SF_17.drawString(modeSetting.getName(), moduleButton.tab.getPosX() + 3, (float) (y + 6), 0xFFFFFFFF, true);
                String current = modeSetting.getMode().toUpperCase();
                SF_17.drawString(current, moduleButton.tab.getPosX() + 97 - SF_17.stringWidth(current), y + 7.0f, new Color(255, 255, 255, 255).getRGB(), true);
            }
        } else if (setting instanceof KeybindSetting) {
            KeybindSetting keybindSetting = (KeybindSetting) setting;
            Gui.drawRect((int) moduleButton.tab.getPosX() + 1, y + 3, (int) moduleButton.tab.getPosX() + 99, y + 14, new Color(0, 0, 0, 50).getRGB());
            SF_18.drawString(setting.getName(), moduleButton.tab.getPosX() + 3, y + 6, 0xFFFFFFFF, true);
            String key = "[" + (keybindSetting.isBinding() ? ".." : keybindSetting.getKeyName()) + "]";
            SF_18.drawString(key, moduleButton.tab.getPosX() + 97.5f - SF_18.stringWidth(key), y + 5.5f, new Color(170, 170, 170).getRGB(), true);
        }
    }

    public int getY() {
        int y = moduleButton.y + 14;
        for (NovolineSettingElement dropDownSetting : moduleButton.settings) {
            if (!dropDownSetting.setting.isVisible()) continue;
            if (dropDownSetting == this) {
                break;
            } else {
                y += dropDownSetting.getHeight();
            }
        }
        return y;
    }

    public int getHeight() {
        if (!setting.isVisible()) return 0;
        if (setting instanceof ModeSetting && opened) {
            return 17 + ((ModeSetting) setting).getModes().size() * 17;
        }
        return 15;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!setting.isVisible()) return;

        if (isHovered(mouseX, mouseY)) {
            if (setting instanceof BooleanSetting) {
                if (mouseButton == 0) {
                    ((BooleanSetting) setting).toggle();
                }
            } else if (setting instanceof NumberSetting) {
                if (mouseButton == 0) {
                    dragging = true;
                }
            } else if (setting instanceof ModeSetting) {
                ModeSetting modeSetting = (ModeSetting) setting;
                if (mouseButton == 1) {
                    opened = !opened;
                } else if (mouseButton == 0) {
                    if (opened) {
                        opened = false;
                    } else {
                        modeSetting.cycle();
                    }
                }
            } else if (setting instanceof KeybindSetting) {
                KeybindSetting keybindSetting = (KeybindSetting) setting;
                if (mouseButton == 0) {
                    keybindSetting.setBinding(!keybindSetting.isBinding());
                }
            }
        }

        // Sub-elements click for opened ModeSetting
        if (setting instanceof ModeSetting && opened) {
            ModeSetting modeSetting = (ModeSetting) setting;
            if (mouseX >= moduleButton.tab.getPosX() && mouseX <= moduleButton.tab.getPosX() + 98) {
                for (int i = 0; i < modeSetting.getModes().size(); i++) {
                    int v = getY() + 17 + i * 17;
                    if (mouseY >= v && mouseY <= v + 17) {
                        modeSetting.setMode(modeSetting.getModes().get(i));
                        break;
                    }
                }
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (setting instanceof KeybindSetting) {
            KeybindSetting keybindSetting = (KeybindSetting) setting;
            if (keybindSetting.isBinding()) {
                if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                    keybindSetting.setKeyCode(Keyboard.KEY_NONE);
                } else {
                    keybindSetting.setKeyCode(keyCode);
                }
                keybindSetting.setBinding(false);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) dragging = false;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        if (!setting.isVisible()) return false;
        int y = getY();
        if (setting instanceof NumberSetting) {
            return mouseX >= moduleButton.tab.getPosX() + 1 && mouseY >= y + 3 && mouseX <= moduleButton.tab.getPosX() + 99 && mouseY <= y + 14;
        } else if (setting instanceof BooleanSetting) {
            return mouseX >= moduleButton.tab.getPosX() + 89 && mouseY >= y + 4 && mouseX <= moduleButton.tab.getPosX() + 99 && mouseY <= y + 14
                    || (mouseX >= moduleButton.tab.getPosX() && mouseX <= moduleButton.tab.getPosX() + 99 && mouseY >= y + 3 && mouseY <= y + 14);
        } else if (setting instanceof KeybindSetting) {
            KeybindSetting keybindSetting = (KeybindSetting) setting;
            String key = "[" + keybindSetting.getKeyName() + "]";
            return mouseX >= moduleButton.tab.getPosX() + 1 && mouseX <= moduleButton.tab.getPosX() + 99 && mouseY >= y + 3 && mouseY <= y + 14;
        } else {
            return mouseX >= moduleButton.tab.getPosX() && mouseY >= y && mouseX <= moduleButton.tab.getPosX() + 99 && mouseY <= y + 15;
        }
    }
}

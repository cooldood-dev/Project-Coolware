package coolware.gui.clickgui;

import coolware.font.CFontRenderer;
import coolware.font.FontManager;
import coolware.module.Module;
import coolware.module.visuals.Theme;
import coolware.setting.BooleanSetting;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.setting.Setting;
import coolware.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

public class ModuleButton {
    private final Module module;
    private final CategoryPanel parent;

    // Number slider dragging
    private NumberSetting draggingSlider = null;

    // Keybind editing state
    private boolean isBinding = false;

    // Theme input editing states
    private boolean isEditingPrimaryHex;
    private boolean isEditingSecondaryHex;
    private String hexBuffer = "";

    public static int getRowHeight() {
        return Math.max(18, ClickGuiFonts.bold().getHeight() + 7);
    }

    public static int getSettingRowHeight() {
        return Math.max(16, ClickGuiFonts.small().getHeight() + 6);
    }

    public ModuleButton(Module module, CategoryPanel parent) {
        this.module = module;
        this.parent = parent;
    }

    public int getHeight() {
        int rowH = getRowHeight();
        if (!module.isExpanded()) {
            return rowH;
        }
        int settingRowH = getSettingRowHeight();
        int total = rowH;
        total += settingRowH; // Keybind setting item row
        if (module instanceof Theme) {
            total += settingRowH; // Hex editor info line
        }
        for (Setting s : module.getSettings()) {
            if (s.isVisible()) {
                total += settingRowH;
            }
        }
        return total;
    }

    public void draw(int x, int y, int width, int mouseX, int mouseY) {
        int rowH = getRowHeight();
        int settingRowH = getSettingRowHeight();
        boolean hovered = isHovered(x, y, width, rowH, mouseX, mouseY);
        Theme theme = Theme.getInstance();
        int accentColor = theme != null ? theme.getColor(0) : 0xFFE00000;
        CFontRenderer titleFont = ClickGuiFonts.bold();
        CFontRenderer settingFont = ClickGuiFonts.small();
        CFontRenderer valueFont = ClickGuiFonts.small();
        CFontRenderer metaFont = ClickGuiFonts.regular();

        // Module row background
        int bgColor;
        if (module.isToggled()) {
            bgColor = hovered ? 0xDD2A1015 : 0xBB200A0E; // Subtle rich dark red active state
        } else {
            bgColor = hovered ? 0xDD1A1B22 : 0x99121318; // Sleek dark surface when inactive
        }

        // Draw module button background (with 3px horizontal padding from panel border)
        RenderUtil.drawRoundedRect(x + 3, y + 1, width - 6, rowH - 2, 3.0F, bgColor);

        // Module text
        String displayText = module.getName();
        int textColor = module.isToggled() ? 0xFFFFFFFF : (hovered ? 0xFFDDDDDD : 0xFFA0A0A0);

        if (module instanceof Theme) {
            Theme t = (Theme) module;
            displayText = "Theme [" + t.getColorMode().name() + "]";
            if (module.isToggled()) {
                textColor = accentColor;
            }
        }

        int textX = x + 7;

        // Right side indicators: theme color badges or dropdown arrow
        int rightMargin = 7;
        if (module instanceof Theme) {
            Theme t = (Theme) module;
            RenderUtil.drawBorderedRect(x + width - 26, y + 4, x + width - 16, y + rowH - 4, 1.0F, 0xFFFFFFFF, t.getPrimaryColor());
            RenderUtil.drawBorderedRect(x + width - 14, y + 4, x + width - 4, y + rowH - 4, 1.0F, 0xFFFFFFFF, t.getSecondaryColor());
            rightMargin = 30;
        } else {
            String expIcon = module.isExpanded() ? "v" : ">";
            if (module.getKeyCode() != Keyboard.KEY_NONE) {
                expIcon = "[" + module.getKeyName() + "] " + expIcon;
            }
            int expColor = module.isExpanded() ? accentColor : 0xFF707070;
            int expWidth = getStringWidth(metaFont, expIcon);
            int expX = x + width - expWidth - 7;
            float metaY = y + (rowH - getFontHeight(metaFont)) / 2.0F + 1;
            if (metaFont != null) {
                metaFont.drawStringWithShadow(expIcon, expX, metaY, expColor);
            } else {
                Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(expIcon, expX, y + (rowH - 8) / 2, expColor);
            }
            rightMargin = expWidth + 12;
        }

        // Clip title so it never touches or overlaps the right badge / keybind text
        int maxTitleWidth = width - 14 - rightMargin;
        drawTrimmedString(titleFont, displayText, textX, y + (rowH - getFontHeight(titleFont)) / 2.0F + 1, textColor, maxTitleWidth);

        // Draw sub-settings dropdown if module is expanded (right-clicked)
        if (module.isExpanded()) {
            int settingY = y + rowH;

            // Handle slider dragging if active
            if (draggingSlider != null) {
                double min = draggingSlider.getMin();
                double max = draggingSlider.getMax();
                double diff = Math.min(width - 16, Math.max(0, mouseX - (x + 8)));
                double percent = diff / (width - 16);
                double newValue = min + percent * (max - min);
                draggingSlider.setValue(newValue);
            }

            // Keybind row
            boolean kHovered = mouseX >= x + 4 && mouseX <= x + width - 4 && mouseY >= settingY && mouseY < settingY + settingRowH;
            int kBg = kHovered ? 0xDD1B1C24 : 0xBB111217;
            RenderUtil.drawRoundedRect(x + 4, settingY, width - 8, settingRowH - 1, 2.0F, kBg);

            String kText = isBinding ? "Bind: [Press Key...]" : ("Bind: [" + (module.getKeyCode() == Keyboard.KEY_NONE ? "NONE" : module.getKeyName()) + "]");
            int kColor = isBinding ? 0xFFFF4455 : (module.getKeyCode() == Keyboard.KEY_NONE ? 0xFF888888 : accentColor);
            drawTrimmedString(settingFont, kText, x + 8, settingY + (settingRowH - getFontHeight(settingFont)) / 2.0F + 1, kColor, width - 16);
            settingY += settingRowH;

            if (module instanceof Theme) {
                Theme t = (Theme) module;
                RenderUtil.drawRoundedRect(x + 4, settingY, width - 8, settingRowH - 1, 2.0F, 0xBB0F1014);
                String hexLine = isEditingPrimaryHex ? ("Pri: " + hexBuffer + "_") :
                        isEditingSecondaryHex ? ("Sec: " + hexBuffer + "_") :
                                ("Pri: " + t.getPrimaryHex() + " | Sec: " + t.getSecondaryHex());
                int hColor = (isEditingPrimaryHex || isEditingSecondaryHex) ? 0xFFFF4455 : 0xFFAAAAAA;
                drawTrimmedString(settingFont, hexLine, x + 8, settingY + (settingRowH - getFontHeight(settingFont)) / 2.0F + 1, hColor, width - 16);
                settingY += settingRowH;
            }

            for (Setting s : module.getSettings()) {
                if (!s.isVisible()) continue;
                boolean sHovered = mouseX >= x + 4 && mouseX <= x + width - 4 && mouseY >= settingY && mouseY < settingY + settingRowH;
                int sBg = sHovered ? 0xDD1B1C24 : 0xBB111217;
                RenderUtil.drawRoundedRect(x + 4, settingY, width - 8, settingRowH - 1, 2.0F, sBg);

                if (s instanceof BooleanSetting) {
                    BooleanSetting bs = (BooleanSetting) s;
                    String text = bs.getName();
                    int tColor = bs.isEnabled() ? 0xFFFFFFFF : 0xFF777777;

                    // Compact modern pill toggle switch
                    int toggleW = 14;
                    int toggleH = 7;
                    int toggleX = x + width - toggleW - 8;
                    int toggleY = settingY + (settingRowH - toggleH) / 2;

                    int trackColor = bs.isEnabled() ? accentColor : 0xFF2A2B33;
                    int knobColor = bs.isEnabled() ? 0xFFFFFFFF : 0xFF888888;
                    RenderUtil.drawRoundedRect(toggleX, toggleY, toggleW, toggleH, 3.5F, trackColor);

                    int knobX = bs.isEnabled() ? toggleX + toggleW - 6 : toggleX + 1;
                    RenderUtil.drawRoundedRect(knobX, toggleY + 1, 5, 5, 2.5F, knobColor);

                    int maxTextW = toggleX - (x + 8) - 4;
                    drawTrimmedString(settingFont, text, x + 8, settingY + (settingRowH - getFontHeight(settingFont)) / 2.0F + 1, tColor, maxTextW);

                } else if (s instanceof NumberSetting) {
                    NumberSetting ns = (NumberSetting) s;
                    double min = ns.getMin();
                    double max = ns.getMax();
                    double val = ns.getValue();
                    double percent = (val - min) / (max - min);

                    String text = ns.getName();
                    String valText = (ns.getIncrement() == 1.0 ? String.valueOf(ns.getValueInt()) : String.format("%.2f", val));
                    if (valText.endsWith(".00")) valText = valText.substring(0, valText.length() - 3);
                    else if (valText.endsWith("0")) valText = valText.substring(0, valText.length() - 1);

                    int valWidth = getStringWidth(valueFont, valText);
                    int valX = x + width - valWidth - 8;
                    drawString(valueFont, valText, valX, settingY + (settingRowH - getFontHeight(valueFont)) / 2.0F + 1, accentColor);

                    int maxLabelW = valX - (x + 8) - 4;
                    drawTrimmedString(settingFont, text, x + 8, settingY + (settingRowH - getFontHeight(settingFont)) / 2.0F + 1, 0xFFDDDDDD, maxLabelW);

                    // Slider track bar
                    int sliderX = x + 8;
                    int sliderW = width - 16;
                    int sliderY = settingY + settingRowH - 3;
                    RenderUtil.drawRoundedRect(sliderX, sliderY, sliderW, 2.0F, 1.0F, 0xFF252730);
                    RenderUtil.drawRoundedRect(sliderX, sliderY, (float) (sliderW * Math.max(0.0, Math.min(1.0, percent))), 2.0F, 1.0F, accentColor);

                } else if (s instanceof ModeSetting) {
                    ModeSetting ms = (ModeSetting) s;
                    String text = ms.getName();
                    String modeText = ms.getMode();

                    int modeWidth = getStringWidth(valueFont, modeText);
                    int modeX = x + width - modeWidth - 8;

                    // Mode value text in accent color
                    drawString(valueFont, modeText, modeX, settingY + (settingRowH - getFontHeight(valueFont)) / 2.0F + 1, accentColor);

                    // Setting name clipped gracefully so it NEVER collides or overlaps modeText
                    int maxLabelW = modeX - (x + 8) - 4;
                    drawTrimmedString(settingFont, text, x + 8, settingY + (settingRowH - getFontHeight(settingFont)) / 2.0F + 1, 0xFFDDDDDD, maxLabelW);

                } else if (s instanceof coolware.setting.KeybindSetting) {
                    coolware.setting.KeybindSetting ks = (coolware.setting.KeybindSetting) s;
                    String text = ks.getName();
                    String keyName = ks.isBinding() ? "..." : "[" + ks.getKeyName() + "]";

                    int keyWidth = getStringWidth(valueFont, keyName);
                    int keyX = x + width - keyWidth - 8;

                    int ksColor = ks.isBinding() ? 0xFFFF4455 : accentColor;
                    drawString(valueFont, keyName, keyX, settingY + (settingRowH - getFontHeight(valueFont)) / 2.0F + 1, ksColor);

                    int maxLabelW = keyX - (x + 8) - 4;
                    drawTrimmedString(settingFont, text, x + 8, settingY + (settingRowH - getFontHeight(settingFont)) / 2.0F + 1, 0xFFDDDDDD, maxLabelW);
                }

                settingY += settingRowH;
            }
        }
    }

    private static void drawTrimmedString(CFontRenderer font, String text, float x, float y, int color, int maxWidth) {
        if (text == null) return;
        if (font != null) {
            String rendered = text;
            if (font.getStringWidth(rendered) > maxWidth) {
                while (rendered.length() > 3 && font.getStringWidth(rendered + "..") > maxWidth) {
                    rendered = rendered.substring(0, rendered.length() - 1);
                }
                rendered = rendered + "..";
            }
            font.drawStringWithShadow(rendered, x, y, color);
        } else {
            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            String rendered = text;
            if (fr.getStringWidth(rendered) > maxWidth) {
                while (rendered.length() > 3 && fr.getStringWidth(rendered + "..") > maxWidth) {
                    rendered = rendered.substring(0, rendered.length() - 1);
                }
                rendered = rendered + "..";
            }
            fr.drawStringWithShadow(rendered, x, y, color);
        }
    }

    private static void drawString(CFontRenderer font, String text, float x, float y, int color) {
        if (font != null) {
            font.drawStringWithShadow(text, x, y, color);
        } else {
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, x, y, color);
        }
    }

    private static int getStringWidth(CFontRenderer font, String text) {
        return font != null ? font.getStringWidth(text) : Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
    }

    private static int getFontHeight(CFontRenderer font) {
        return font != null ? font.getHeight() : Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
    }

    public void mouseClicked(int x, int y, int width, int mouseX, int mouseY, int mouseButton) {
        int rowH = getRowHeight();
        int settingRowH = getSettingRowHeight();
        if (isHovered(x, y, width, rowH, mouseX, mouseY)) {
            if (mouseButton == 0) {
                // Left click: Toggle module
                module.toggle();
            } else if (mouseButton == 1) {
                // Right click: OPEN / CLOSE module settings
                module.toggleExpanded();
            } else if (mouseButton == 2) {
                // Middle click: Keybind listener
                isBinding = !isBinding;
            }
            return;
        }

        // If expanded, handle clicks inside sub-settings
        if (module.isExpanded()) {
            int settingY = y + rowH;

            // Keybind row clicked
            if (mouseX >= x && mouseX <= x + width && mouseY >= settingY && mouseY < settingY + settingRowH) {
                isBinding = !isBinding;
                return;
            }
            settingY += settingRowH;

            if (module instanceof Theme) {
                Theme t = (Theme) module;
                if (mouseX >= x && mouseX <= x + width && mouseY >= settingY && mouseY < settingY + settingRowH) {
                    if (mouseButton == 0) {
                        isEditingPrimaryHex = true;
                        isEditingSecondaryHex = false;
                        hexBuffer = t.getPrimaryHex();
                    } else if (mouseButton == 1) {
                        isEditingSecondaryHex = true;
                        isEditingPrimaryHex = false;
                        hexBuffer = t.getSecondaryHex();
                    }
                    return;
                }
                settingY += settingRowH;
            }

            for (Setting s : module.getSettings()) {
                if (!s.isVisible()) continue;
                if (mouseX >= x && mouseX <= x + width && mouseY >= settingY && mouseY < settingY + settingRowH) {
                    if (s instanceof BooleanSetting) {
                        if (mouseButton == 0 || mouseButton == 1) {
                            ((BooleanSetting) s).toggle();
                        }
                    } else if (s instanceof ModeSetting) {
                        if (mouseButton == 0) {
                            ((ModeSetting) s).cycle();
                        }
                    } else if (s instanceof NumberSetting) {
                        if (mouseButton == 0) {
                            draggingSlider = (NumberSetting) s;
                            double min = draggingSlider.getMin();
                            double max = draggingSlider.getMax();
                            double diff = Math.min(width - 16, Math.max(0, mouseX - (x + 8)));
                            double percent = diff / (width - 16);
                            double newValue = min + percent * (max - min);
                            draggingSlider.setValue(newValue);
                        }
                    } else if (s instanceof coolware.setting.KeybindSetting) {
                        coolware.setting.KeybindSetting ks = (coolware.setting.KeybindSetting) s;
                        if (mouseButton == 0 || mouseButton == 1 || mouseButton == 2) {
                            ks.setBinding(!ks.isBinding());
                        }
                    }
                    return;
                }
                settingY += settingRowH;
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0 && draggingSlider != null) {
            draggingSlider = null;
        }
    }

    public boolean mouseScrolled(int x, int y, int width, int mouseX, int mouseY, int wheelDelta) {
        if (!module.isExpanded()) {
            return false;
        }

        int rowH = getRowHeight();
        int settingRowH = getSettingRowHeight();
        int settingY = y + rowH + settingRowH; // Skip the keybind row
        if (module instanceof Theme) {
            settingY += settingRowH; // Skip theme hex row
        }

        for (Setting setting : module.getSettings()) {
            if (!setting.isVisible()) continue;
            if (mouseX >= x && mouseX <= x + width && mouseY >= settingY && mouseY < settingY + settingRowH
                    && setting instanceof NumberSetting) {
                NumberSetting numberSetting = (NumberSetting) setting;
                int steps = Math.max(1, Math.abs(wheelDelta) / 120);
                double direction = wheelDelta > 0 ? 1.0 : -1.0;
                numberSetting.setValue(numberSetting.getValue() + direction * numberSetting.getIncrement() * steps);
                return true;
            }
            settingY += settingRowH;
        }
        return false;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (isBinding) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
                module.setKeyCode(Keyboard.KEY_NONE);
            } else if (keyCode != Keyboard.KEY_NONE) {
                module.setKeyCode(keyCode);
            }
            isBinding = false;
            return;
        }

        for (Setting setting : module.getSettings()) {
            if (setting instanceof coolware.setting.KeybindSetting) {
                coolware.setting.KeybindSetting ks = (coolware.setting.KeybindSetting) setting;
                if (ks.isBinding()) {
                    if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
                        ks.setKeyCode(Keyboard.KEY_NONE);
                    } else if (keyCode != Keyboard.KEY_NONE) {
                        ks.setKeyCode(keyCode);
                    }
                    ks.setBinding(false);
                    return;
                }
            }
        }

        if (isEditingPrimaryHex || isEditingSecondaryHex) {
            Theme t = (Theme) module;
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                if (isEditingPrimaryHex) {
                    t.setPrimaryHex(hexBuffer);
                    isEditingPrimaryHex = false;
                } else {
                    t.setSecondaryHex(hexBuffer);
                    isEditingSecondaryHex = false;
                }
                hexBuffer = "";
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                isEditingPrimaryHex = false;
                isEditingSecondaryHex = false;
                hexBuffer = "";
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (hexBuffer.length() > 0) {
                    hexBuffer = hexBuffer.substring(0, hexBuffer.length() - 1);
                }
            } else if ("0123456789ABCDEFabcdef#".indexOf(typedChar) != -1 && hexBuffer.length() < 7) {
                hexBuffer += Character.toUpperCase(typedChar);
            }
        }
    }

    public boolean hasActiveInput() {
        if (isBinding || isEditingPrimaryHex || isEditingSecondaryHex) return true;
        for (Setting s : module.getSettings()) {
            if (s instanceof coolware.setting.KeybindSetting && ((coolware.setting.KeybindSetting) s).isBinding()) {
                return true;
            }
        }
        return false;
    }

    private boolean isHovered(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}

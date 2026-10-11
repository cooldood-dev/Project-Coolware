package coolware.gui.clickgui.novoline;

import coolware.Coolware;
import coolware.font.novoline.impl.Fonts;
import coolware.module.Category;
import coolware.module.Module;
import net.minecraft.client.gui.Gui;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static coolware.font.novoline.impl.Fonts.SF.SF_20.SF_20;

public class NovolineTab {

    private final Category category;
    private float posX;
    private float posY;
    public boolean dragging, opened = true;
    public List<NovolineModuleButton> modules = new CopyOnWriteArrayList<NovolineModuleButton>();

    public NovolineTab(Category category, float posX, float posY) {
        this.category = category;
        this.posX = posX;
        this.posY = posY;
        if (category != null && Coolware.getInstance() != null && Coolware.getInstance().getModuleManager() != null) {
            for (Module module : Coolware.getInstance().getModuleManager().getModulesByCategory(category)) {
                modules.add(new NovolineModuleButton(module, this));
            }
        }
    }

    public void drawScreen(int mouseX, int mouseY) {
        String l = "";
        if (category != null) {
            if (category == Category.COMBAT) {
                l = "D";
            } else if (category == Category.MOVEMENT) {
                l = "A";
            } else if (category == Category.PLAYER) {
                l = "B";
            } else if (category == Category.VISUALS) {
                l = "C";
            } else if (category == Category.CLIENT) {
                l = "F";
            }
        }

        Gui.drawRect((int) posX - 1, (int) posY, (int) posX + 101, (int) posY + 15, new Color(29, 29, 29, 255).getRGB());
        if (!l.isEmpty()) {
            Fonts.ICONFONT.ICONFONT_24.ICONFONT_24.drawString(l, posX + 88, posY + 5, 0xFFFFFFFF);
        }
        String title = category != null ? category.getName() : "Tab";
        SF_20.drawString(title, posX + 4, posY + 4, 0xFFFFFFFF, true);

        if (opened) {
            Gui.drawRect((int) posX - 1, (int) posY + 15, (int) posX + 101, (int) posY + 15 + getTabHeight() + 1, new Color(29, 29, 29, 255).getRGB());
            for (NovolineModuleButton module : modules) {
                module.drawScreen(mouseX, mouseY);
            }
        } else {
            for (NovolineModuleButton module : modules) {
                module.yPerModule = 0;
            }
        }
    }

    public int getTabHeight() {
        int height = 0;
        for (NovolineModuleButton module : modules) {
            height += module.yPerModule;
        }
        return height;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= posX && mouseY >= posY && mouseX <= posX + 101 && mouseY <= posY + 15;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (opened) {
            for (NovolineModuleButton module : modules) {
                module.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        for (NovolineModuleButton module : modules) {
            module.keyTyped(typedChar, keyCode);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isHovered(mouseX, mouseY) && mouseButton == 1) {
            opened = !opened;
            if (opened) {
                for (NovolineModuleButton module : modules) {
                    module.fraction = 0;
                }
            }
        }

        if (opened) {
            for (NovolineModuleButton module : modules) {
                try {
                    module.mouseClicked(mouseX, mouseY, mouseButton);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public float getPosY() {
        return posY;
    }

    public float getPosX() {
        return posX;
    }

    public List<NovolineModuleButton> getModules() {
        return modules;
    }

    public Category getCategory() {
        return category;
    }
}

package coolware.module;

import coolware.setting.Setting;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final Minecraft mc = Minecraft.getMinecraft();

    private final String name;
    private final String description;
    private final Category category;
    private int keyCode;
    private boolean toggled;
    private boolean expanded;
    private final List<Setting> settings = new ArrayList<Setting>();

    public Module(String name, String description, Category category, int keyCode) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keyCode = keyCode;
        this.toggled = false;
        this.expanded = false;
    }

    public void addSetting(Setting setting) {
        if (setting != null && !this.settings.contains(setting)) {
            this.settings.add(setting);
        }
    }

    public List<Setting> getSettings() {
        return settings;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }

    public void toggle() {
        setToggled(!toggled);
    }

    public void setToggled(boolean toggled) {
        if (this.toggled != toggled) {
            this.toggled = toggled;
            if (this.toggled) {
                onEnable();
            } else {
                onDisable();
            }
            if (mc != null && mc.theWorld != null && !this.name.equalsIgnoreCase("ClickGUI")) {
                coolware.notification.NotificationManager.addNotification(
                    "Module",
                    this.name + (toggled ? " \u00A7aEnabled" : " \u00A7cDisabled"),
                    toggled ? coolware.notification.impl.NotificationType.INFO : coolware.notification.impl.NotificationType.WARNING
                );
            }
        }
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onUpdate() {}

    public void onRender2D() {}

    public void onRender3D(float partialTicks) {}

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public boolean isToggled() {
        return toggled;
    }

    public String getKeyName() {
        return keyCode == Keyboard.KEY_NONE ? "None" : Keyboard.getKeyName(keyCode);
    }
}

package com.github.cooldood.modules;

import com.github.cooldood.Main;
import com.github.cooldood.events.Bus;
import com.github.cooldood.events.impl.ModuleStateChangeEvent;
import com.github.cooldood.utils.render.notifications.NotificationManager;
import com.github.cooldood.utils.render.notifications.NotificationType;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Getter
public abstract class Module {
    private boolean enabled;
    private final ArrayList<SubModule> children = new ArrayList<>();

    @Setter
    private boolean open;

    @Setter
    private RegisterModule annotation;

    private int keybind = -1;

    public void setKeybind(int keybind) {
        this.keybind = keybind;

        ModuleManager.saveConfig(Main.baseConfig);
    }

    public void setEnabled(boolean flag) {
        flag = keybind != -1 && holdKeybind ? Keyboard.isKeyDown(keybind) : flag;

        if (enabled != flag) {
            enabled = flag;

            // save config whenever anything changes!
            ModuleManager.saveConfig(Main.baseConfig);

            Bus.post(new ModuleStateChangeEvent(this, flag));

            if (enabled) {
                com.github.cooldood.utils.client.SoundUtil.playSound("/scalehack/sounds/toggleon.wav");
                NotificationManager.post(annotation.name(), "Module enabled", NotificationType.SUCCESS, 3700);
                onEnable();
            } else {
                com.github.cooldood.utils.client.SoundUtil.playSound("/scalehack/sounds/toggleoff.wav");
                NotificationManager.post(annotation.name(), "Module disabled", NotificationType.WARNING, 3700);
                onDisable();
            }
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public String getUniqueKey(String key) {
        return annotation.name() + annotation.category().name() + annotation.description() + key;
    }

    public String arrayListExtraInfo() {
        return "";
    }

    private List<SubModule> getVisibleSubModules() {
        return this.getChildren().stream().filter(SubModule::shouldRender).collect(Collectors.toList());
    }

    @RegisterSubModule(name = "General")
    public SubCategory general = new SubCategory();

    @RegisterSubModule(name = "Hide", description = "Hides this module from the array list")
    public boolean hide = false;

    @RegisterSubModule(name = "Hold Keybind", description = "Module enables if the keybind is held down")
    public boolean holdKeybind = false;

    protected abstract void onEnable();
    protected abstract void onDisable();
}

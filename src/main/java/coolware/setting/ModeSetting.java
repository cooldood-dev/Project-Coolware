package coolware.setting;

import coolware.module.Module;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting {
    private int index;
    private final List<String> modes;

    public ModeSetting(String name, Module parent, String defaultMode, String... modes) {
        super(name, parent);
        this.modes = Arrays.asList(modes);
        this.index = this.modes.indexOf(defaultMode);
        if (this.index == -1) {
            this.index = 0;
        }
    }

    public String getMode() {
        return modes.get(index);
    }

    public boolean is(String mode) {
        return getMode().equalsIgnoreCase(mode);
    }

    public void cycle() {
        if (index < modes.size() - 1) {
            index++;
        } else {
            index = 0;
        }
    }

    public void setMode(String mode) {
        for (int i = 0; i < modes.size(); i++) {
            if (modes.get(i).equalsIgnoreCase(mode)) {
                this.index = i;
                return;
            }
        }
    }

    public List<String> getModes() {
        return modes;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = Math.max(0, Math.min(modes.size() - 1, index));
    }
}

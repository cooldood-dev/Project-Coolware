package coolware.setting;

import coolware.module.Module;

public class BooleanSetting extends Setting {
    private boolean enabled;

    public BooleanSetting(String name, Module parent, boolean enabled) {
        super(name, parent);
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggle() {
        this.enabled = !this.enabled;
    }
}

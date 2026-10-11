package coolware.setting;

import coolware.module.Module;

public abstract class Setting {
    private final String name;
    private final Module parent;
    private boolean hidden = false;

    public Setting(String name, Module parent) {
        this.name = name;
        this.parent = parent;
        if (parent != null) {
            parent.addSetting(this);
        }
    }

    public String getName() {
        return name;
    }

    public Module getParent() {
        return parent;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isVisible() {
        return !hidden;
    }
}

package coolware.setting;

import coolware.module.Module;
import org.lwjgl.input.Keyboard;

public class KeybindSetting extends Setting {
    private int keyCode;
    private boolean isBinding = false;

    public KeybindSetting(String name, Module parent, int defaultKey) {
        super(name, parent);
        this.keyCode = defaultKey;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public boolean isBinding() {
        return isBinding;
    }

    public void setBinding(boolean binding) {
        isBinding = binding;
    }

    public String getKeyName() {
        if (keyCode == Keyboard.KEY_NONE || keyCode == 0) {
            return "None";
        }
        String name = Keyboard.getKeyName(keyCode);
        return name == null ? "Key " + keyCode : name;
    }

    public boolean isBound() {
        return keyCode != Keyboard.KEY_NONE && keyCode != 0;
    }

    public boolean isKeyDown() {
        if (!isBound()) return false;
        try {
            return Keyboard.isKeyDown(keyCode);
        } catch (Exception e) {
            return false;
        }
    }
}

package coolware.module.movement;

import coolware.module.Category;
import coolware.module.Module;
import org.lwjgl.input.Keyboard;

public class NoSlow extends Module {
    public NoSlow() {
        super("NoSlow", "Prevents item use slowdown", Category.MOVEMENT, Keyboard.KEY_NONE);
    }
}

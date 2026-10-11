package coolware.module.visuals;

import coolware.module.Category;
import coolware.module.Module;
import org.lwjgl.input.Keyboard;

public class ItemPhysics extends Module {
    public ItemPhysics() {
        super("ItemPhysics", "Enables realistic physics for dropped items", Category.VISUALS, Keyboard.KEY_NONE);
    }
}

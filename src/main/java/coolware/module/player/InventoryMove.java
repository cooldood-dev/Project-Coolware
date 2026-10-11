package coolware.module.player;

import coolware.module.Category;
import coolware.module.Module;
import coolware.module.player.invmove.*;
import coolware.setting.ModeSetting;
import net.minecraft.network.Packet;
import net.minecraft.util.MovementInput;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

public class InventoryMove extends Module {

    private final ModeSetting mode;
    private final Map<String, ModuleMode> modeMap = new HashMap<String, ModuleMode>();

    public InventoryMove() {
        super("InventoryMove", "Lets you move while your inventory is open", Category.PLAYER, Keyboard.KEY_NONE);

        mode = new ModeSetting("Mode", this, "Watchdog", "Watchdog", "Blink", "Slowdown", "Grim", "Vulcan");
        addSetting(mode);

        modeMap.put("Watchdog", new Watchdoginvmove("Watchdog", this));
        modeMap.put("Blink", new BlinkInvmove("Blink", this));
        modeMap.put("Slowdown", new SlowdownInvmove("Slowdown", this));
        modeMap.put("Grim", new GrimInvmove("Grim", this));
        modeMap.put("Vulcan", new VulcanInvmove("Vulcan", this));
    }

    public ModeSetting getMode() {
        return mode;
    }

    public ModuleMode getCurrentModeHandler() {
        if (mode == null) return null;
        return modeMap.get(mode.getMode());
    }

    @Override
    public void onEnable() {
        ModuleMode handler = getCurrentModeHandler();
        if (handler != null) {
            handler.onEnable();
        }
    }

    @Override
    public void onDisable() {
        ModuleMode handler = getCurrentModeHandler();
        if (handler != null) {
            handler.onDisable();
        }
    }

    @Override
    public void onUpdate() {
        ModuleMode handler = getCurrentModeHandler();
        if (handler != null) {
            handler.onUpdate();
        }
    }

    public static boolean onPacketSend(Packet<?> packet) {
        InventoryMove invMove = coolware.Coolware.getInstance().getModuleManager().getModule(InventoryMove.class);
        if (invMove != null && invMove.isToggled()) {
            ModuleMode handler = invMove.getCurrentModeHandler();
            if (handler != null) {
                return handler.onPacketSend(packet);
            }
        }
        return false;
    }

    public static void onMovementInput(MovementInput input) {
        InventoryMove invMove = coolware.Coolware.getInstance().getModuleManager().getModule(InventoryMove.class);
        if (invMove != null && invMove.isToggled()) {
            ModuleMode handler = invMove.getCurrentModeHandler();
            if (handler != null) {
                handler.onMoveInput(input);
            }
        }
    }
}

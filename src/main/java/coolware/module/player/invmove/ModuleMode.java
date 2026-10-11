package coolware.module.player.invmove;

import coolware.module.Module;
import net.minecraft.client.Minecraft;

public abstract class ModuleMode {
    public final Minecraft mc = Minecraft.getMinecraft();
    private final String modeName;
    private final Module superModule;

    public ModuleMode(String modeName, Module module) {
        this.modeName = modeName;
        this.superModule = module;
    }

    public String getModeName() {
        return modeName;
    }

    public Module getSuperModule() {
        return superModule;
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onUpdate() {}

    public boolean onPacketSend(net.minecraft.network.Packet<?> packet) {
        return false;
    }

    public void onMoveInput(net.minecraft.util.MovementInput input) {}
}

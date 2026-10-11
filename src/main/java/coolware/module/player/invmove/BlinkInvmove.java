package coolware.module.player.invmove;

import coolware.module.Module;
import coolware.util.PacketUtil;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class BlinkInvmove extends ModuleMode {

    private final List<Packet<?>> packets = new ArrayList<Packet<?>>();

    public BlinkInvmove(String modeName, Module module) {
        super(modeName, module);
    }

    @Override
    public void onUpdate() {
        if (mc.currentScreen instanceof GuiChat) return;
        updateKeyBindings();
    }

    @Override
    public boolean onPacketSend(Packet<?> packet) {
        if (packet instanceof C0DPacketCloseWindow) {
            for (Packet<?> p : packets) {
                PacketUtil.sendPacketNoEvent(p);
            }
            packets.clear();
        }
        return false;
    }

    private void updateKeyBindings() {
        if (mc.gameSettings == null) return;
        mc.gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        mc.gameSettings.keyBindBack.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        mc.gameSettings.keyBindLeft.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
        mc.gameSettings.keyBindRight.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
        mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
    }

    @Override
    public void onEnable() {
        packets.clear();
    }

    @Override
    public void onDisable() {
        packets.clear();
    }
}

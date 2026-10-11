package coolware.module.player.invmove;

import coolware.module.Module;
import coolware.util.PacketUtil;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.util.MovementInput;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class GrimInvmove extends ModuleMode {

    private boolean magic = false;
    private int magic2 = 0;
    private final List<Packet<?>> packets = new ArrayList<Packet<?>>();

    public GrimInvmove(String modeName, Module module) {
        super(modeName, module);
    }

    @Override
    public void onEnable() {
        packets.clear();
        magic = false;
        magic2 = 0;
    }

    @Override
    public void onDisable() {
        packets.clear();
        magic = false;
        magic2 = 0;
    }

    @Override
    public void onMoveInput(MovementInput input) {
        if (!(mc.currentScreen instanceof GuiInventory)) {
            if (magic && input != null) {
                input.moveForward = 0;
                input.moveStrafe = 0;
            }
        }
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.gameSettings == null) return;
        if (mc.currentScreen instanceof GuiChat) return;

        updateKeyBindings();

        if (mc.currentScreen instanceof GuiInventory) {
            magic = true;
        } else {
            if (magic) {
                magic2++;
                resetMovementKeys();
                if (magic2 > 3) {
                    for (Packet<?> p : packets) {
                        PacketUtil.sendPacketNoEvent(p);
                    }
                    packets.clear();
                    magic2 = 0;
                    magic = false;
                } else {
                    PacketUtil.sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                }
            }
        }
    }

    @Override
    public boolean onPacketSend(Packet<?> packet) {
        if (packet instanceof C16PacketClientStatus || packet instanceof C0EPacketClickWindow) {
            packets.add(packet);
            return true; // Cancel event
        }
        return false;
    }

    private void resetMovementKeys() {
        if (mc.gameSettings == null) return;
        mc.gameSettings.keyBindForward.pressed = false;
        mc.gameSettings.keyBindLeft.pressed = false;
        mc.gameSettings.keyBindRight.pressed = false;
        mc.gameSettings.keyBindBack.pressed = false;
    }

    private void updateKeyBindings() {
        if (mc.gameSettings == null) return;
        mc.gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        mc.gameSettings.keyBindBack.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        mc.gameSettings.keyBindLeft.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
        mc.gameSettings.keyBindRight.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
        mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
    }
}

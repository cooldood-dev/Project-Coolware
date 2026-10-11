package coolware.module.player.invmove;

import coolware.module.Module;
import coolware.module.combat.KillAura;
import coolware.module.movement.Scaffold;
import coolware.module.player.InvManager;
import coolware.util.PacketUtil;
import net.minecraft.block.BlockContainer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

public class Watchdoginvmove extends ModuleMode {

    public boolean using = false;
    private int ticksSinceUsed = 0;
    public int ticksSinceChestClick = 0;

    public Watchdoginvmove(String modeName, Module module) {
        super(modeName, module);
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.gameSettings == null) return;
        ticksSinceUsed++;
        if (mc.currentScreen instanceof GuiChat) return;

        InvManager manager = coolware.Coolware.getInstance().getModuleManager().getModule(InvManager.class);

        if (mc.thePlayer.isUsingItem()) {
            ticksSinceUsed = 0;
            if (manager != null) manager.stop = true;
        } else {
            if (ticksSinceUsed == 4) {
                if (manager != null) manager.stop = false;
            }
        }

        ticksSinceChestClick++;
        updateKeyBindings();

        if (mc.currentScreen instanceof GuiChest) {
            PotionEffect speedEffect = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed);
            float slowMultiplier = (speedEffect != null) ? 0.22f : 0.65f;

            mc.thePlayer.motionX *= slowMultiplier;
            mc.thePlayer.motionZ *= slowMultiplier;
        }

        boolean nogui = manager != null && manager.isSpoofEnabled();
        if ((mc.currentScreen instanceof GuiInventory || (manager != null && manager.isToggled() && nogui && mc.currentScreen == null && ticksSinceChestClick > 6)) && sendConditions()) {
            switch (mc.thePlayer.ticksExisted % 4) {
                case 0:
                    PacketUtil.sendPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
                    break;
                case 1:
                    PacketUtil.sendPacket(new C0DPacketCloseWindow());
                    break;
            }
        }
    }

    @Override
    public boolean onPacketSend(Packet<?> packet) {
        InvManager manager = coolware.Coolware.getInstance().getModuleManager().getModule(InvManager.class);

        if (packet instanceof C0DPacketCloseWindow) {
            if (manager != null) manager.stop2 = true;
        } else if (packet instanceof C16PacketClientStatus) {
            if (manager != null) manager.stop2 = false;
        }

        if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement c08 = (C08PacketPlayerBlockPlacement) packet;
            BlockPos pos = c08.getPosition();
            if (mc.theWorld != null && pos != null && mc.theWorld.getBlockState(pos).getBlock() instanceof BlockContainer) {
                ticksSinceChestClick = 0;
            }
        }
        return false;
    }

    private boolean sendConditions() {
        Module scaffold = coolware.Coolware.getInstance().getModuleManager().getModule(Scaffold.class);
        boolean scaffoldEnabled = scaffold != null && scaffold.isToggled();
        return !scaffoldEnabled && !mc.thePlayer.isUsingItem() && KillAura.target == null;
    }

    private void updateKeyBindings() {
        if (mc.gameSettings == null) return;
        mc.gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        mc.gameSettings.keyBindBack.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        mc.gameSettings.keyBindLeft.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
        mc.gameSettings.keyBindRight.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());

        if (mc.currentScreen instanceof GuiChest) {
            mc.gameSettings.keyBindJump.pressed = false;
        } else {
            mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
        }
    }
}

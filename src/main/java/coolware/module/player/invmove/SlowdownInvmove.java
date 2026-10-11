package coolware.module.player.invmove;

import coolware.module.Module;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.input.Keyboard;

public class SlowdownInvmove extends ModuleMode {

    public SlowdownInvmove(String modeName, Module module) {
        super(modeName, module);
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.gameSettings == null) return;
        if (mc.currentScreen instanceof GuiChat) return;

        updateKeyBindings();

        if (mc.currentScreen instanceof GuiInventory || mc.currentScreen instanceof GuiChest) {
            PotionEffect speedEffect = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed);
            float slowMultiplier = speedEffect != null ? 0.22f : 0.65f;

            mc.thePlayer.motionX *= slowMultiplier;
            mc.thePlayer.motionZ *= slowMultiplier;
        }
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

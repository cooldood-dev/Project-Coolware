package coolware.command.impl;

import coolware.Coolware;
import coolware.command.Command;
import coolware.config.ConfigManager;
import coolware.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

public class BindCommand extends Command {
    public BindCommand() {
        super("bind", "Binds a module to a key", "b", "setkey");
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        if (args.length < 2) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: .bind <module> <key|none>"));
            return;
        }

        String modName = args[0];
        String keyName = args[1].toUpperCase();

        Module module = Coolware.getInstance().getModuleManager().getModuleByName(modName);
        if (module == null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Module not found: " + modName));
            return;
        }

        if (keyName.equalsIgnoreCase("NONE") || keyName.equalsIgnoreCase("CLEAR") || keyName.equalsIgnoreCase("0")) {
            module.setKeyCode(Keyboard.KEY_NONE);
            ConfigManager.saveConfig(ConfigManager.DEFAULT_CONFIG);
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Unbound key for " + EnumChatFormatting.WHITE + module.getName()));
            return;
        }

        int key = Keyboard.getKeyIndex(keyName);
        if (key == Keyboard.KEY_NONE) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Invalid key name: " + keyName));
            return;
        }

        module.setKeyCode(key);
        ConfigManager.saveConfig(ConfigManager.DEFAULT_CONFIG);
        mc.thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GREEN + "Bound " + EnumChatFormatting.WHITE + module.getName() +
                EnumChatFormatting.GREEN + " to " + EnumChatFormatting.YELLOW + Keyboard.getKeyName(key)));
    }
}

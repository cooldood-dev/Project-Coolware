package coolware.command.impl;

import coolware.command.Command;
import coolware.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.List;

public class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "Saves, loads, or lists configuration files", "cfg", "c");
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        if (args.length == 0) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: .config <save|load|list> [name]"));
            return;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("save")) {
            String name = args.length > 1 ? args[1] : ConfigManager.DEFAULT_CONFIG;
            if (ConfigManager.saveConfig(name)) {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.GREEN + "Successfully saved config: " + EnumChatFormatting.YELLOW + name + ".cfg"));
            } else {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.RED + "Failed to save config: " + name));
            }
        } else if (sub.equals("load")) {
            String name = args.length > 1 ? args[1] : ConfigManager.DEFAULT_CONFIG;
            if (ConfigManager.loadConfig(name)) {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.GREEN + "Successfully loaded config: " + EnumChatFormatting.YELLOW + name + ".cfg"));
            } else {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.RED + "Config file not found: " + name + ".cfg"));
            }
        } else if (sub.equals("list")) {
            List<String> list = ConfigManager.listConfigs();
            if (list.isEmpty()) {
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "No config files found."));
            } else {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.AQUA + "Available configs (" + list.size() + "): " +
                        EnumChatFormatting.WHITE + String.join(", ", list)));
            }
        } else {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Unknown sub-command: " + sub + ". Use save, load, or list."));
        }
    }
}

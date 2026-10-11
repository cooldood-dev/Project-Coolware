package coolware.command.impl;

import coolware.Coolware;
import coolware.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Displays available client commands", "h", "?");
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        mc.thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.AQUA + "=== " + Coolware.NAME + " Commands ==="));
        for (Command cmd : Coolware.getInstance().getCommandManager().getCommands()) {
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.YELLOW + "." + cmd.getName() +
                    EnumChatFormatting.GRAY + " - " + cmd.getDescription()));
        }
    }
}

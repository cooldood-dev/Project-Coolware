package coolware.command.impl;

import coolware.Coolware;
import coolware.command.Command;
import coolware.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle", "Toggles a module on or off", "t");
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        if (args.length < 1) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: .toggle <module>"));
            return;
        }

        Module mod = Coolware.getInstance().getModuleManager().getModuleByName(args[0]);
        if (mod == null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Module not found: " + args[0]));
            return;
        }

        mod.toggle();
        mc.thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GRAY + mod.getName() + " is now " +
                (mod.isToggled() ? EnumChatFormatting.GREEN + "enabled" : EnumChatFormatting.RED + "disabled")));
    }
}

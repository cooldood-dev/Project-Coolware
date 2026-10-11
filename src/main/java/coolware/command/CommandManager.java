package coolware.command;

import coolware.command.impl.BindCommand;
import coolware.command.impl.ConfigCommand;
import coolware.command.impl.HelpCommand;
import coolware.command.impl.ToggleCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {
    private final List<Command> commands = new ArrayList<Command>();
    private final String prefix = ".";

    public CommandManager() {
        registerCommand(new BindCommand());
        registerCommand(new ConfigCommand());
        registerCommand(new ToggleCommand());
        registerCommand(new HelpCommand());
    }

    public void registerCommand(Command cmd) {
        commands.add(cmd);
    }

    public boolean handleChatMessage(String rawMessage) {
        if (rawMessage == null || !rawMessage.startsWith(prefix)) {
            return false;
        }

        String withoutPrefix = rawMessage.substring(prefix.length()).trim();
        if (withoutPrefix.isEmpty()) {
            return false;
        }

        String[] parts = withoutPrefix.split(" ");
        String commandName = parts[0];
        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        Command command = getCommand(commandName);
        if (command != null) {
            command.execute(args);
        } else {
            net.minecraft.client.Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                            net.minecraft.util.EnumChatFormatting.RED + "Unknown command: ." + commandName + ". Type .help for a list of commands."));
        }

        return true;
    }

    public Command getCommand(String name) {
        for (Command cmd : commands) {
            if (cmd.getName().equalsIgnoreCase(name)) {
                return cmd;
            }
            for (String alias : cmd.getAliases()) {
                if (alias.equalsIgnoreCase(name)) {
                    return cmd;
                }
            }
        }
        return null;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public String getPrefix() {
        return prefix;
    }
}

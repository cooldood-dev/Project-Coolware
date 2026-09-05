package com.github.cooldood.commands.impl;

import com.github.cooldood.commands.Command;
import com.github.cooldood.commands.CommandManager;
import com.github.cooldood.utils.minecraft.ChatUtil;

import java.util.List;
import java.util.stream.Collectors;

public class HelpCommand extends Command {

    @Override
    public String name() {
        return "help";
    }

    @Override
    public boolean execute(String[] args) {
        if (args[0].equals("commands")) {
            displayPossibleCommands(CommandManager.getCommands());
        } else {
            ChatUtil.prefixMessage("Mod Information: ");
            ChatUtil.chat(
                    "&7Modules are found using fuzzy search, this means you can type in any part of the module name to find it!" +
                            "\n&7Spaces are used to split each command argument, so you can't name configs with spaces!" +
                            "\n&7Use \".help commands\" &7to see a list of commands.");
        }

        return true;
    }

    public static void displayPossibleCommands(List<Command> commands) {
        ChatUtil.prefixMessage("&cCommands: ");
        ChatUtil.chat(commands.stream().map(HelpCommand::getCommandUsageString).collect(Collectors.joining("\n")));
    }

    public static String getCommandUsageString(Command command) {
        StringBuilder commandUsages = new StringBuilder();
        for (String usage : command.usage())
            commandUsages.append("\n&c.").append(command.name()).append(" &f").append(usage);
        return ((commandUsages.length() == 0) ? "\n&c." + command.name() : commandUsages.toString());
    }

    @Override
    public String[] usage() {
        return new String[] {
                "",
                "commands"
        };
    }
}

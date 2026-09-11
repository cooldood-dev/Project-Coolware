package com.github.cooldood.commands;

import com.github.cooldood.Main;
import com.github.cooldood.commands.impl.HelpCommand;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.ChatUtil;
import lombok.Getter;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.network.play.client.C01PacketChatMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// i dont like this command manager but im sadly too lazy to make the improvements it needs.
public class CommandManager {
    @Getter
    private static final ArrayList<Command> commands = new ArrayList<>();

    public static void init() {
        for (Class<?> clazz : C.reflections.getSubTypesOf(Command.class)) {
            try {
                Main.LOGGER.info("Registering command: {}", clazz.getSimpleName());
                commands.add((Command) clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                Main.LOGGER.error("Failed to register command: {}", clazz.getSimpleName(), e);
            }
        }
    }

    @SubscribeEvent
    public static void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof C01PacketChatMessage) {
            String message = ((C01PacketChatMessage) event.packet).getMessage();

            if (isMessageCommand(message)) {
                event.setCancelled(true);
                handleCommand(message.substring(1));
            }
        }
    }

    public static boolean isMessageCommand(String s) {
        return s.startsWith(".");
    }

    public static List<ExtractedResult> getPossibleCommands(String commandName) {
        List<String> commandNames = commands.stream().map(command -> command.name().toLowerCase()).collect(Collectors.toList());

        // i love fuzzysearch
        return FuzzySearch.extractAll(commandName, commandNames, 40);
    }

    private static void handleCommand(String commandText) {
        String commandName = commandText.split(" ")[0].toLowerCase();

        // i love fuzzysearch
        Optional<ExtractedResult> best = getPossibleCommands(commandName).stream().max(Comparator.comparingInt(ExtractedResult::getScore));
        if (!best.isPresent()) {
            ChatUtil.prefixMessage("&cCouldn't find command: " + commandName);
            ChatUtil.prefixMessage("&cUse &f\".help\" &cfor information about the commands system!");
            ChatUtil.prefixMessage("&cUse &f\".help commands\" &cfor a list of commands!");
            return;
        }

        Command command = commands.get(best.get().getIndex());
        String[] args = commandText.substring(commandName.length()).trim().split(" ");

        if (!command.execute(args)) {
            ChatUtil.prefixMessage("&cInvalid Usage");
            for (String usage : command.usage()) {
                ChatUtil.prefixMessage("&c." + command.name() + " &f" + usage);
            }
        }
    }

    public static String gatherArgs(String[] args, int split) {
        String remainingArgs = "";
        for (int i = split; i < args.length; i++) {
            remainingArgs += args[i] + " ";
        }

        return remainingArgs.trim();
    }

    private static String lastInput = "";
    private static String lastTabbedWord = "";
    private static int commandIndex = -1;

    public static void resetTabProgress() {
        lastInput = "";
        lastTabbedWord = "";
        commandIndex = -1;
    }

    public static void handleCommandTabbed(GuiTextField inputField) {
        // code stolen from net.minecraft.client.gui.GuiChat.autocompletePlayerNames
        int i = inputField.func_146197_a(-1, inputField.getCursorPosition(), false);
        String wordsAfterCursor = inputField.getText().substring(i).toLowerCase();
        String wordsBeforeCursor = inputField.getText().substring(0, inputField.getCursorPosition());

        // scale official code wowowow
        String currentWord = wordsAfterCursor.split(" ")[0];
        String commandName = wordsBeforeCursor.split(" ")[0].substring(1);

        if (!currentWord.equals(lastInput)) {
            resetTabProgress();
            lastInput = currentWord;
        }

        if (lastTabbedWord.isEmpty()) lastTabbedWord = "." + commandName;

        if (CommandManager.isMessageCommand(currentWord)) {
            handleCommandNameTabbed(lastTabbedWord, inputField);
        }

        // todo: allow tabbing all commands, need to make a function in every command to get possible tab options </3
    }

    private static void handleCommandNameTabbed(String commandName, GuiTextField inputField) {
        List<Command> possibleCommands = CommandManager.getPossibleCommands(commandName)
                .stream()
                .sorted(Comparator.comparingInt(e -> -e.getScore()))
                .map(e -> CommandManager.getCommands().get(e.getIndex()))
                .collect(Collectors.toList());

        if (possibleCommands.isEmpty() && commandIndex == -1) {
            ChatUtil.prefixMessage("No commands found for this input: " + commandName);
            HelpCommand.displayPossibleCommands(CommandManager.getCommands());
        }
        else {
            if (commandIndex == -1) {
                HelpCommand.displayPossibleCommands(possibleCommands);
                commandIndex++;
            }

            if (possibleCommands.isEmpty()) possibleCommands = CommandManager.getCommands();

            commandIndex %= possibleCommands.size();

            Command command = possibleCommands.get(commandIndex);
            ChatUtil.prefixMessage("Usage for command &f" + command.name() + "&c:" + HelpCommand.getCommandUsageString(command));

            lastInput = "." + command.name();
            lastTabbedWord = commandName;
            inputField.setText(lastInput);
        }

        commandIndex++;
    }
}

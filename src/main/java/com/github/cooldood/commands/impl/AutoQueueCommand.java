package com.github.cooldood.commands.impl;

import com.github.cooldood.commands.Command;
import com.github.cooldood.modules.impl.client.AutoQueueHandler;

public class AutoQueueCommand extends Command {
    @Override
    public String name() {
        return "autoqueue";
    }

    @Override
    public boolean execute(String[] args) {
        if (args.length == 0) return false;

        String action = args[0].toLowerCase();
        switch (action) {
            case "on":
                AutoQueueHandler.setEnabled(true);
                return true;
            case "off":
                AutoQueueHandler.setEnabled(false);
                return true;
            case "mode":
                if (args.length < 2) return false;
                AutoQueueHandler.setGameMode(args[1]);
                return true;
            case "delay":
                if (args.length < 2) return false;
                try {
                    AutoQueueHandler.setDelayMs(Integer.parseInt(args[1]));
                    return true;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            default:
                return false;
        }
    }

    @Override
    public String[] usage() {
        return new String[] {
                "on",
                "off",
                "mode bedwars|skywars",
                "delay <ms>"
        };
    }
}

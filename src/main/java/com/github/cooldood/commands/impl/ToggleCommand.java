package com.github.cooldood.commands.impl;

import com.github.cooldood.commands.Command;
import com.github.cooldood.modules.Module;
import com.github.cooldood.utils.client.FuzzySearchUtil;

public class ToggleCommand extends Command {
    @Override
    public String name() {
        return "toggle";
    }

    @Override
    public boolean execute(String[] args) {
        if (args.length < 1) return false;

        Module module = FuzzySearchUtil.findModule(args[0]);
        if (module == null) return false;

        if (args.length < 2) {
            module.toggle();
            return true;
        }
        else {

            return true;
        }
    }

    @Override
    public String[] usage() {
        return new String[]{"<module>"};
    }
}

package com.github.cooldood.commands.impl;

import com.github.cooldood.commands.Command;
import com.github.cooldood.utils.alts.SessionUtil;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.ChatUtil;

public class ChangeCommand extends Command {

    @Override
    public String name() {
        return "change";
    }

    @Override
    public boolean execute(String[] args) {
        if (args.length < 2) return false;

        switch (args[0]) {
            case "name":
                if (args.length < 3 || !args[2].equals("confirm")) return false;

                ChatUtil.prefixMessage(SessionUtil.changeName(C.mc.getSession().getToken(), args[1]));
                return true;

            case "skin":
                if (args.length >= 3) {
                    ChatUtil.prefixMessage(SessionUtil.changeSkin(C.mc.getSession().getToken(), args[1], args[2].equals("slim") ? "slim" : "classic"));
                }
                else {
                    ChatUtil.prefixMessage(SessionUtil.changeSkin(C.mc.getSession().getToken(), args[1]));
                }

                return true;
        }

        return false;
    }

    @Override
    public String[] usage() {
        return new String[] {
                "name <new name> confirm",
                "skin <skin url>",
                "skin <skin url> [slim/classic]",
        };
    }
}

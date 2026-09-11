package com.github.cooldood.commands.impl;

import com.github.cooldood.commands.Command;
import com.github.cooldood.utils.alts.Login;

public class LoginCommand extends Command {

    @Override
    public String name() {
        return "login";
    }

    @Override
    public boolean execute(String[] args) {
        if (args.length < 1) return false;

        switch (args[0]) {
            case "microsoft":
                Login.AltTypes.Microsoft.action.run();
                return true;
            case "refreshtoken":
                Login.AltTypes.Refresh_Token.action.run();
                return true;
            case "session":
                Login.AltTypes.Session.action.run();
                return true;
            case "cookie":
                Login.AltTypes.Cookie.action.run();
                return true;
        }

        return false;
    }

    @Override
    public String[] usage() {
        return new String[] {
                "microsoft [opens browser]",
                "refreshtoken [from clipboard]",
                "session [from clipboard]",
                "cookie [file / file path from clipboard]",
        };
    }
}

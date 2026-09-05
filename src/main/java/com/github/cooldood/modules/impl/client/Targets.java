package com.github.cooldood.modules.impl.client;

import com.github.cooldood.modules.*;
import lombok.AllArgsConstructor;

@RegisterModule(
        name = "Targets",
        description = "Manages target priorities and filters.",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class Targets extends Module {
    @RegisterSubModule(name = "Anti Bot")
    public static boolean antiBot = false;

    @RegisterSubModule(name = "Bot Ping", parent = "Anti Bot")
    public static Bot_Mode botPingMode = Bot_Mode.Hypixel;

    @AllArgsConstructor
    public enum Bot_Mode {
        // .ordinal -1, mb. keeping for later ig idk
        Negative(-1),
        Hypixel(0),
        Cracked(1);

        public final int ping;
    }

    @RegisterSubModule(name = "Teams Check")
    public static boolean teamsCheck = false;
    @RegisterSubModule(name = "Show Visuals", parent = "Teams Check")
    public static boolean showVisuals = true;
    @RegisterSubModule(name = "Client Teams", parent = "Teams Check")
    public static boolean clientCheck = true;
    @RegisterSubModule(name = "Colour Teams", parent = "Teams Check")
    public static boolean colourTeams = true;

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {
        ModuleManager.getModule(Targets.class).setEnabled(true);
    }
}

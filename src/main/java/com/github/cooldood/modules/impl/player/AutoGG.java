package com.github.cooldood.modules.impl.player;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import lombok.AllArgsConstructor;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.EnumChatFormatting;

@RegisterModule(
        name = "Auto GG",
        description = "Provides Auto GG functionality for the client.",
        category = Category.PLAYER
)
public class AutoGG extends Module {
    @RegisterSubModule(name = "Outgoing")
    public static ggMessages ggMessage = ggMessages.gg;

    @AllArgsConstructor
    public enum ggMessages {
        gg("gg"),
        GG("GG"),
        gf("gf"),
        Good_Game("Good Game"),
        Good_Fight("Good Fight"),
        Good_Round("Good Round! :D");

        public final String message;
    }

    @SubscribeEvent
    public static void onChat(PacketEvent.Receive event) {
        if (!(event.packet instanceof S02PacketChat)) return;

        S02PacketChat packet = (S02PacketChat) event.packet;
        String message = EnumChatFormatting.getTextWithoutFormattingCodes(packet.getChatComponent().getUnformattedText());

        for (String regex : REGEX) {
            if (message.matches(regex)) {
                C.p().sendChatMessage(ggMessage.message);
                return;
            }
        }
    }

    // stole from https://static.sk1er.club/autogg/regex_triggers_3.json
    private static final String[] REGEX = new String[] {
            "^ +1st Killer - ?\\[?\\w*\\+*\\]? \\w+ - \\d+(?: Kills?)?$",
            "^ *1st (?:Place ?)?(?:-|:)? ?\\[?\\w*\\+*\\]? \\w+(?: : \\d+| - \\d+(?: Points?)?| - \\d+(?: x .)?| \\(\\w+ .{1,6}\\) - \\d+ Kills?|: \\d+:\\d+| - \\d+ (?:Zombie )?(?:Kills?|Blocks? Destroyed)| - \\[LINK\\])?$",
            "^ +Winn(?:er #1 \\(\\d+ Kills\\): \\w+ \\(\\w+\\)|er(?::| - )(?:Hiders|Seekers|Defenders|Attackers|PLAYERS?|MURDERERS?|Red|Blue|RED|BLU|\\w+)(?: Team)?|ers?: ?\\[?\\w*\\+*\\]? \\w+(?:, ?\\[?\\w*\\+*\\]? \\w+)?|ing Team ?[\\:-] (?:Animals|Hunters|Red|Green|Blue|Yellow|RED|BLU|Survivors|Vampires))$",
            "^ +Alpha Infected: \\w+ \\(\\d+ infections?\\)$",
            "^ +Murderer: \\w+ \\(\\d+ Kills?\\)$",
            "^ +You survived \\d+ rounds!$",
            "^ +(?:UHC|SkyWars|Bridge|Sumo|Classic|OP|MegaWalls|Bow|NoDebuff|Blitz|Combo|Bow Spleef) (?:Duel|Doubles|3v3|4v4|Teams|Deathmatch|2v2v2v2|3v3v3v3)? ?- \\d+:\\d+$",
            "^ +They captured all wools!$",
            "^ +Game over!$",
            "^ +[\\d\\.]+k?/[\\d\\.]+k? \\w+$",
            "^ +(?:Criminal|Cop)s won the game!$",
            "^ +\\[?\\w*\\+*\\]? \\w+ - \\d+ Final Kills$",
            "^ +Zombies - \\d*:?\\d+:\\d+ \\(Round \\d+\\)$",
            "^ +. YOUR STATISTICS .$",
            "^ {36}Winner(s?)$",
            "^ {21}Bridge CTF [a-zA-Z]+ - \\d\\d:\\d\\d$"
    };

    @Override
    protected void onEnable() {
    }


    @Override
    protected void onDisable() {
    }
}

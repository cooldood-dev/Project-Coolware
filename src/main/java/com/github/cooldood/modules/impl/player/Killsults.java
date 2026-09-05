package com.github.cooldood.modules.impl.player;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.events.impl.WorldUnloadEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.client.C;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.EnumChatFormatting;

@RegisterModule(
        name = "Killsults",
        description = "Automatically insults killed players.",
        category = Category.PLAYER
)
public class Killsults extends Module {

    private static final String[] MESSAGES = {
        "Subscribe to @cooldood67420 on yt for more coolware updates",
        "Sorry babe, that skill could work against ambient or astralis not coolware",
        "coolware mogs all",
        "If a vibecode can screw u up, who are you?",
        "Screwed by cooldood",
        "Developer = cooldood",
        "Gemini and Claude VS watchdog",
        "million dollar anticheat vs indian kid",
        "YVL by coolware",
        "Greetings from Folk Valley",
        "This one sponsored by the Communist Hacks Party",
        "Hello Micheal Stetson"
    };

    private static int currentIndex = 0;

    @SubscribeEvent
    public static void onWorldUnload(WorldUnloadEvent event) {
        currentIndex = 0;
    }

    @SubscribeEvent
    public static void onChat(PacketEvent.Receive event) {
        if (!(event.packet instanceof S02PacketChat)) return;

        S02PacketChat packet = (S02PacketChat) event.packet;
        String message = EnumChatFormatting.getTextWithoutFormattingCodes(packet.getChatComponent().getUnformattedText());
        String messageStr = packet.getChatComponent().toString();

        // Reset per game on Hypixel
        if (messageStr.contains("ClickEvent{action=RUN_COMMAND, value='/play ") || messageStr.contains("Want to play again?")) {
            currentIndex = 0;
        }

        // Basic Hypixel kill messages regex check
        // Examples: 
        // "Player1 was killed by Player2."
        // "Player1 was thrown into the void by Player2."
        // "Player1 was struck down by Player2."
        
        String myName = C.p().getName();
        
        if (message.contains("by " + myName) || message.contains("to " + myName)) {
            // Send message and increment index
            C.p().sendChatMessage(MESSAGES[currentIndex]);
            currentIndex = (currentIndex + 1) % MESSAGES.length;
        }
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
}

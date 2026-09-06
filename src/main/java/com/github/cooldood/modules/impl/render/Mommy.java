package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.SoundUtil;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.EnumChatFormatting;

import java.util.regex.Pattern;

@RegisterModule(
        name = "Mommy",
        description = "Plays audio on your kills, bed breaks, and victories.",
        category = Category.RENDER,
        enabledByDefault = true
)
public class Mommy extends Module {

    @RegisterSubModule(name = "Kill Sound", description = "Play audio on your own kills")
    public static boolean killSound = true;

    @RegisterSubModule(name = "Bed Break Sound", description = "Play audio on your own bed breaks")
    public static boolean bedBreakSound = true;

    @RegisterSubModule(name = "Victory Sound", description = "Play audio on game victory/win")
    public static boolean victorySound = true;

    private static long lastKillTime = 0;
    private static long lastBedTime = 0;
    private static long lastVictoryTime = 0;

    // Trigger patterns for Hypixel victories
    private static final Pattern WIN_MESSAGE_PATTERN = Pattern.compile(
            "(?i).*\\b(1st place!?|#1!?|victory!?|winner!?|won the game|you won(?: the game)?|team wins!?|hypixel victory)\\b.*"
    );

    @SubscribeEvent
    public static void onPacketReceive(PacketEvent.Receive event) {
        if (C.p() == null || C.mc.thePlayer == null) return;

        // Check chat messages
        if (event.packet instanceof S02PacketChat) {
            S02PacketChat packet = (S02PacketChat) event.packet;
            String message = EnumChatFormatting.getTextWithoutFormattingCodes(
                    packet.getChatComponent().getUnformattedText());

            if (message == null || message.trim().isEmpty()) return;

            String myName = C.p().getName();
            long now = System.currentTimeMillis();

            // 1. Check Victory / Win
            if (victorySound && (now - lastVictoryTime > 4000)) {
                String lowerMsg = message.toLowerCase();
                if (!message.contains(":") && WIN_MESSAGE_PATTERN.matcher(lowerMsg).matches()) {
                    lastVictoryTime = now;
                    playVictorySound();
                    return;
                }
            }

            // 2. Check Own Bed Break
            if (bedBreakSound && (now - lastBedTime > 2000)) {
                String upper = message.toUpperCase();
                if (upper.contains("BED DESTRUCTION") || upper.contains("BED WAS DESTROYED")) {
                    if (message.contains("by " + myName) || message.contains("to " + myName) || message.contains("by " + myName + "!")) {
                        lastBedTime = now;
                        playBedSound();
                        return;
                    }
                }
            }

            // 3. Check Own Kills (Exact detection mechanism from Killsults module)
            if (killSound && (now - lastKillTime > 500)) {
                if (message.contains("by " + myName) || message.contains("to " + myName) || message.startsWith("You have killed ")) {
                    lastKillTime = now;
                    playKillSound();
                    return;
                }
            }
        }

        // Check Title packets (e.g. Hypixel "VICTORY!" title)
        if (event.packet instanceof S45PacketTitle) {
            S45PacketTitle titlePacket = (S45PacketTitle) event.packet;
            if (victorySound && titlePacket.getMessage() != null) {
                String titleText = EnumChatFormatting.getTextWithoutFormattingCodes(
                        titlePacket.getMessage().getUnformattedText());
                if (titleText != null && titleText.toUpperCase().contains("VICTORY")) {
                    long now = System.currentTimeMillis();
                    if (now - lastVictoryTime > 4000) {
                        lastVictoryTime = now;
                        playVictorySound();
                    }
                }
            }
        }
    }

    public static void playKillSound() {
        SoundUtil.playSound("/coolware/sounds/mommy_kill.mp3");
    }

    public static void playBedSound() {
        SoundUtil.playSound("/coolware/sounds/mommy_bed.mp3");
    }

    public static void playVictorySound() {
        SoundUtil.playSound("/coolware/sounds/mommy_win.mp3");
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
}

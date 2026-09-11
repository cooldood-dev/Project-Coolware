package com.github.cooldood.modules.impl.client;

import com.github.cooldood.Main;
import com.github.cooldood.utils.client.C;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class AutoQueueHandler {
    public enum State {
        IDLE,
        QUEUED,
        IN_GAME,
        LOBBY
    }

    private static final Pattern BEDWARS_WIN = Pattern.compile("(?i).*(victory|you won|winner|1st place|won the game|team wins).*");
    private static final Pattern BEDWARS_LOSS = Pattern.compile("(?i).*(defeat|you lost|game over|eliminated|you were eliminated|bed destroyed).*");
    private static final Pattern SKYWARS_WIN = Pattern.compile("(?i).*(victory|you won|winner|1st place|won the game).*");
    private static final Pattern SKYWARS_LOSS = Pattern.compile("(?i).*(defeat|you lost|game over|eliminated|you were eliminated).*");
    private static final Pattern RETURN_TO_LOBBY = Pattern.compile("(?i).*(return(ing)? to lobby|sending you back to the lobby|sending you to the lobby|back to the lobby).*");

    private static final AtomicBoolean requeuePending = new AtomicBoolean(false);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AutoQueueScheduler");
        t.setDaemon(true);
        return t;
    });

    public static boolean enabled = true;
    public static String gameMode = "bedwars";
    public static int delayMs = 4000;
    private static volatile State state = State.IDLE;
    private static long lastTriggerAt = 0L;

    public static void loadConfig(Configuration config) {
        enabled = config.getBoolean("enabled", "autoqueue", true, "Enable AutoQueue");
        gameMode = config.getString("gameMode", "autoqueue", "bedwars", "Game mode: bedwars or skywars").toLowerCase(Locale.ROOT);
        delayMs = config.getInt("delayMs", "autoqueue", 4000, 0, 60000, "Delay before requeueing in milliseconds");
    }

    public static void saveConfig() {
        if (Main.autoQueueConfig == null) return;
        Main.autoQueueConfig.get("autoqueue", "enabled", enabled).set(enabled);
        Main.autoQueueConfig.get("autoqueue", "gameMode", gameMode).set(gameMode);
        Main.autoQueueConfig.get("autoqueue", "delayMs", delayMs).set(delayMs);
        if (Main.autoQueueConfig.hasChanged()) Main.autoQueueConfig.save();
    }

    public static State getState() {
        return state;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        Main.LOGGER.debug("AutoQueue enabled={}", value);
        saveConfig();
    }

    public static void setGameMode(String mode) {
        if (!"bedwars".equalsIgnoreCase(mode) && !"skywars".equalsIgnoreCase(mode)) return;
        gameMode = mode.toLowerCase(Locale.ROOT);
        Main.LOGGER.debug("AutoQueue mode={}", gameMode);
        saveConfig();
    }

    public static void setDelayMs(int value) {
        delayMs = Math.max(0, value);
        Main.LOGGER.debug("AutoQueue delayMs={}", delayMs);
        saveConfig();
    }

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!enabled || C.mc.thePlayer == null) return;

        String raw = event.message == null ? "" : event.message.getUnformattedText();
        String text = raw.replaceAll("§.", "").trim();
        if (text.isEmpty()) return;

        if (state == State.QUEUED || state == State.IN_GAME) {
            if (RETURN_TO_LOBBY.matcher(text).matches()) {
                transition(State.LOBBY, "lobby message");
            }
            return;
        }

        boolean matched = matchesEndMessage(text);
        if (!matched) return;

        long now = System.currentTimeMillis();
        if (now - lastTriggerAt < 1500L) return;
        lastTriggerAt = now;

        transition(State.QUEUED, "end-of-game chat");
        if (requeuePending.compareAndSet(false, true)) {
            scheduler.schedule(() -> C.mc.addScheduledTask(() -> {
                try {
                    if (C.mc.thePlayer == null || !enabled) return;
                    if (state != State.QUEUED && state != State.LOBBY) return;
                    String command = "skywars".equalsIgnoreCase(gameMode) ? "/play skywars" : "/play bedwars";
                    C.mc.thePlayer.sendChatMessage(command);
                    transition(State.IN_GAME, "sent " + command);
                } finally {
                    requeuePending.set(false);
                }
            }), delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private boolean matchesEndMessage(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if ("skywars".equalsIgnoreCase(gameMode)) {
            return SKYWARS_WIN.matcher(lower).matches()
                    || SKYWARS_LOSS.matcher(lower).matches()
                    || RETURN_TO_LOBBY.matcher(lower).matches();
        }
        return BEDWARS_WIN.matcher(lower).matches()
                || BEDWARS_LOSS.matcher(lower).matches()
                || RETURN_TO_LOBBY.matcher(lower).matches();
    }

    private static void transition(State next, String reason) {
        if (state != next) {
            state = next;
            Main.LOGGER.debug("AutoQueue state -> {} ({})", next, reason);
        }
    }
}

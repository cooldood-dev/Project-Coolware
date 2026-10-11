package coolware.module.miscellaneous;

import coolware.module.Category;
import coolware.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class Killsults extends Module {

    private static final String[] LINES = {
        "Coolware on top baby",
        "Mogged by coolware",
        "We dont do beef anymore, so no insults to ambient and astralis :) But you know that coolware mogs all",
        "Indian kid vs watchdog",
        "cooldood79420 on yt",
        "Maybe ur skill could work against BS clients but NOT COOLWARE",
        "Remember, u just lost to a vibecoded client",
        "Gemini cooked on this one",
        "Reports say that Drake LOVES coolware just like he likes lil kids",
        "Communist Hacks Party mogs all cheating servers",
        "Donate to coolware via LTC, Le4zqaxEcWovkpxq2wkrA1GwbLgbmpfbTA",
        "GGs lil bro"
    };

    private static Killsults instance;
    private int currentIndex = 0;

    // Last target tracked when attacked by player
    private EntityLivingBase lastTarget = null;
    private long lastTargetTime = 0L;

    // Chat kill indicators matching popular servers (Hypixel, Bedwars, Skywars, Vanilla, etc.)
    private final String[] killChatPatterns = {
        "by &+",
        "was killed by &+",
        "was &+'s final",
        "knocked into the void by &+",
        "thrown into the void by &+",
        "thrown off the edge by &+",
        "was slain by &+",
        "was struck down by &+",
        "was obliterated by &+",
        "was shot by &+",
        "beaten to death by &+",
        "was thrown into the void by &+",
        "got shoved into the void by &+"
    };

    public Killsults() {
        super("Killsults", "Automatically types insults in chat after getting a kill.", Category.CLIENT, Keyboard.KEY_NONE);
        instance = this;
    }

    public static Killsults getInstance() {
        return instance;
    }

    public static void onGameReset() {
        if (instance != null) {
            instance.currentIndex = 0;
            instance.lastTarget = null;
            instance.lastTargetTime = 0L;
        }
    }

    public static void onTargetAttacked(Entity entity) {
        if (instance != null && instance.isToggled() && entity instanceof EntityLivingBase) {
            instance.lastTarget = (EntityLivingBase) entity;
            instance.lastTargetTime = System.currentTimeMillis();
        }
    }

    public static void onEntityStatus(Entity entity, byte status) {
        if (instance == null || !instance.isToggled() || entity == null || mc.thePlayer == null) return;
        if (status == 3) { // Entity death status
            if (entity == instance.lastTarget && (System.currentTimeMillis() - instance.lastTargetTime) <= 6000L) {
                if (entity != mc.thePlayer) {
                    instance.onKillConfirmed();
                    instance.lastTarget = null;
                }
            }
        }
    }

    public static void onChatMessage(String unformattedText) {
        if (instance == null || !instance.isToggled() || unformattedText == null || mc.thePlayer == null) return;
        String playerName = mc.thePlayer.getName();
        if (playerName == null || playerName.isEmpty()) return;

        // Skip own messages or messages typed by the player
        if (unformattedText.startsWith(playerName + ":") || unformattedText.startsWith("<" + playerName + ">")) {
            return;
        }

        // Avoid bed break announcements or shop messages
        if (unformattedText.contains("Bed") && (unformattedText.contains("destroyed") || unformattedText.contains("broken"))) {
            return;
        }

        for (String pattern : instance.killChatPatterns) {
            String check = pattern.replace("&+", playerName);
            if (unformattedText.contains(check)) {
                instance.onKillConfirmed();
                break;
            }
        }
    }

    private synchronized void onKillConfirmed() {
        if (!isToggled() || mc.thePlayer == null) return;

        if (currentIndex < 0 || currentIndex >= LINES.length) {
            currentIndex = 0;
        }

        String msg = LINES[currentIndex];
        currentIndex = (currentIndex + 1) % LINES.length;

        if (msg != null && !msg.trim().isEmpty()) {
            mc.thePlayer.sendChatMessage(msg);
        }
    }

    @Override
    public void onEnable() {
        // Resets target context
        lastTarget = null;
        lastTargetTime = 0L;
    }
}

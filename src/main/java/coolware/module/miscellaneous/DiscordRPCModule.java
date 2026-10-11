package coolware.module.miscellaneous;

import coolware.Coolware;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.BooleanSetting;
import coolware.util.DiscordIPC;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class DiscordRPCModule extends Module {
    public static final String APP_ID = "1557611090584797214";

    private final BooleanSetting showServer = new BooleanSetting("Show Server", this, true);
    private final BooleanSetting showUsername = new BooleanSetting("Show Username", this, true);

    private final DiscordIPC ipc = new DiscordIPC();
    private long startTime = 0L;
    private long lastUpdate = 0L;
    private Thread workerThread = null;

    public DiscordRPCModule() {
        super("DiscordRPC", "Displays rich presence activity on Discord", Category.CLIENT, Keyboard.KEY_NONE);
    }

    @Override
    public void onEnable() {
        startTime = System.currentTimeMillis() / 1000L;
        startWorker();
    }

    @Override
    public void onDisable() {
        stopWorker();
        ipc.close();
    }

    private void startWorker() {
        if (workerThread != null && workerThread.isAlive()) return;
        workerThread = new Thread(() -> {
            while (isToggled()) {
                try {
                    if (!ipc.isConnected()) {
                        ipc.connect(APP_ID);
                    }
                    if (ipc.isConnected()) {
                        updatePresence();
                    }
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {}
            }
            ipc.close();
        }, "Coolware-DiscordRPC");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void stopWorker() {
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }

    private void updatePresence() {
        Minecraft mc = Minecraft.getMinecraft();
        String state = "Idling";
        String details = "Coolware v" + Coolware.VERSION;

        if (mc.thePlayer != null) {
            if (mc.isSingleplayer()) {
                state = "Singleplayer";
            } else if (mc.getCurrentServerData() != null) {
                state = showServer.isEnabled() ? ("Playing on " + mc.getCurrentServerData().serverIP) : "Multiplayer";
            } else {
                state = "In Game";
            }

            if (showUsername.isEnabled() && mc.getSession() != null) {
                details = mc.getSession().getUsername() + " | v" + Coolware.VERSION;
            }
        } else {
            state = "Main Menu";
        }

        ipc.sendPresence(state, details, "logo", "Coolware v" + Coolware.VERSION, startTime);
    }
}

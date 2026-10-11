package coolware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlinkUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Queue<Packet<?>> blinkedSentPackets = new ConcurrentLinkedQueue<Packet<?>>();
    private static final Queue<Packet<?>> blinkedReceivedPackets = new ConcurrentLinkedQueue<Packet<?>>();

    private static int blinkOutgoingIndex = -1;
    private static int blinkIncomingIndex = -1;

    public static boolean onPacketSent(Packet<?> packet) {
        if (blinkOutgoingIndex > -1) {
            blinkedSentPackets.add(packet);
            return true; // Cancel original send
        }
        return false;
    }

    public static boolean onPacketReceived(Packet<?> packet) {
        if (blinkIncomingIndex > -1) {
            blinkedReceivedPackets.add(packet);
            return true; // Cancel original receive
        }
        return false;
    }

    public static boolean isBlinking(boolean sent, boolean received) {
        return (sent && !blinkedSentPackets.isEmpty()) || (received && !blinkedReceivedPackets.isEmpty());
    }

    public static void pushBlink(boolean sent, boolean received) {
        pushBlink(sent, received, null);
    }

    public static void pushBlink(boolean sent, boolean received, Packet<?> packet) {
        if (mc.isSingleplayer()) return;

        if (sent) {
            blinkOutgoingIndex = Math.max(0, blinkOutgoingIndex + 1);
            if (packet != null) blinkedSentPackets.add(packet);
        }

        if (received) {
            blinkIncomingIndex = Math.max(0, blinkIncomingIndex + 1);
            if (packet != null) blinkedReceivedPackets.add(packet);
        }
    }

    public static void popBlink(boolean sent, boolean received) {
        blinkOutgoingIndex -= sent ? 1 : 0;
        blinkIncomingIndex -= received ? 1 : 0;

        if (sent && blinkOutgoingIndex <= -1) {
            Packet<?> packet;
            while ((packet = blinkedSentPackets.poll()) != null) {
                try {
                    if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null) {
                        mc.getNetHandler().getNetworkManager().sendPacket(packet);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (received && blinkIncomingIndex <= -1) {
            Packet<?> packet;
            while ((packet = blinkedReceivedPackets.poll()) != null) {
                try {
                    if (mc.getNetHandler() != null) {
                        processReceivedPacket(packet);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void processReceivedPacket(Packet packet) {
        if (mc.getNetHandler() != null) {
            packet.processPacket(mc.getNetHandler());
        }
    }

    public static void disableBlink(boolean sent, boolean received) {
        if (sent) {
            blinkOutgoingIndex = -1;
            blinkedSentPackets.clear();
        }

        if (received) {
            blinkIncomingIndex = -1;
            blinkedReceivedPackets.clear();
        }
    }

    public static void reset() {
        while (isBlinking(true, true)) {
            popBlink(true, true);
        }
        disableBlink(true, true);
    }
}

package com.github.cooldood.utils.minecraft;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.ClientTickEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.events.impl.RespawnEvent;
import com.github.cooldood.utils.client.C;
import net.minecraft.network.Packet;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlinkUtil {
    private final static Queue<Packet<?>> blinkedSentPackets = new ConcurrentLinkedQueue<>();
    private final static Queue<Packet<?>> blinkedReceivedPackets = new ConcurrentLinkedQueue<>();

    private static int blinkOutgoingIndex = -1;
    private static int blinkIncomingIndex = -1;

    @SubscribeEvent
    public static void onPacketSent(PacketEvent.Send event) {
        if (blinkOutgoingIndex > -1) {
            blinkedSentPackets.add(event.packet);
            event.setCancelled(true);
        }
    }

    @SubscribeEvent(priority = 999)
    public static void onPacketReceived(PacketEvent.Receive event) {
        if (blinkIncomingIndex > -1) {
            blinkedReceivedPackets.add(event.packet);
            event.setCancelled(true);
        }
    }

    @SubscribeEvent(priority = 0)
    public static void resetBlink(RespawnEvent event) {
        while (isBlinking(true, true)) {
            popBlink(true, true);
        }
    }

    @SubscribeEvent
    public static void resetBlink(ClientTickEvent event) {
        if (!C.isInGame()) {
            disableBlink(true, true);
        }
    }

    public static boolean isBlinking(boolean sent, boolean received) {
        return (sent && !blinkedSentPackets.isEmpty()) || (received && !blinkedReceivedPackets.isEmpty());
    }

    public static void pushBlink(boolean sent, boolean received) {
        pushBlink(sent, received, null);
    }

    public static void pushBlink(boolean sent, boolean received, Packet<?> packet) {
        if (C.mc.isSingleplayer()) return;

        if (sent) {
            blinkOutgoingIndex = Math.max(0, blinkOutgoingIndex+1);
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
                    PacketUtil.sendPacket(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (received && blinkIncomingIndex <= -1) {
            Packet<?> packet;

            while ((packet = blinkedReceivedPackets.poll()) != null) {
                try {
                    PacketUtil.receivePacket(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /// clears all the blinked packets and pretends they didnt happen
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
}

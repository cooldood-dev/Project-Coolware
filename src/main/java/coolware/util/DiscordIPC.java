package coolware.util;

import com.google.gson.JsonObject;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Pure Java Discord IPC client using Windows Named Pipes (\\.\pipe\discord-ipc-0..9)
 * No native JNI DLLs required.
 */
public final class DiscordIPC {
    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;
    private static final int OP_PING = 3;
    private static final int OP_PONG = 4;

    private RandomAccessFile pipe = null;
    private boolean connected = false;

    public boolean connect(String clientId) {
        if (connected) return true;
        for (int i = 0; i < 10; i++) {
            File pipeFile = new File("\\\\.\\pipe\\discord-ipc-" + i);
            try {
                pipe = new RandomAccessFile(pipeFile, "rw");
                // Handshake payload
                JsonObject obj = new JsonObject();
                obj.addProperty("v", 1);
                obj.addProperty("client_id", clientId);
                send(OP_HANDSHAKE, obj.toString());

                // Read handshake response
                int op = readIntLE();
                int len = readIntLE();
                if (len > 0) {
                    byte[] data = new byte[len];
                    pipe.readFully(data);
                }
                connected = true;
                return true;
            } catch (Throwable ignored) {
                close();
            }
        }
        return false;
    }

    public synchronized void sendPresence(String state, String details, String largeImageKey, String largeImageText, long startTimestamp) {
        if (!connected || pipe == null) return;
        try {
            JsonObject args = new JsonObject();
            args.addProperty("pid", getPid());

            JsonObject activity = new JsonObject();
            if (state != null && !state.isEmpty()) activity.addProperty("state", state);
            if (details != null && !details.isEmpty()) activity.addProperty("details", details);

            if (startTimestamp > 0) {
                JsonObject timestamps = new JsonObject();
                timestamps.addProperty("start", startTimestamp);
                activity.add("timestamps", timestamps);
            }

            if (largeImageKey != null && !largeImageKey.isEmpty()) {
                JsonObject assets = new JsonObject();
                assets.addProperty("large_image", largeImageKey);
                if (largeImageText != null && !largeImageText.isEmpty()) {
                    assets.addProperty("large_text", largeImageText);
                }
                activity.add("assets", assets);
            }

            args.add("activity", activity);

            JsonObject payload = new JsonObject();
            payload.addProperty("cmd", "SET_ACTIVITY");
            payload.add("args", args);
            payload.addProperty("nonce", UUID.randomUUID().toString());

            send(OP_FRAME, payload.toString());
        } catch (Throwable t) {
            close();
        }
    }

    private void send(int op, String json) throws Exception {
        if (pipe == null) return;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        writeIntLE(op);
        writeIntLE(bytes.length);
        pipe.write(bytes);
    }

    private void writeIntLE(int v) throws Exception {
        pipe.writeByte(v & 0xFF);
        pipe.writeByte((v >> 8) & 0xFF);
        pipe.writeByte((v >> 16) & 0xFF);
        pipe.writeByte((v >> 24) & 0xFF);
    }

    private int readIntLE() throws Exception {
        int b1 = pipe.readUnsignedByte();
        int b2 = pipe.readUnsignedByte();
        int b3 = pipe.readUnsignedByte();
        int b4 = pipe.readUnsignedByte();
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    private int getPid() {
        try {
            String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            int idx = jvmName.indexOf('@');
            if (idx > 0) return Integer.parseInt(jvmName.substring(0, idx));
        } catch (Throwable ignored) {}
        return 1000;
    }

    public synchronized void close() {
        connected = false;
        if (pipe != null) {
            try {
                pipe.close();
            } catch (Throwable ignored) {}
            pipe = null;
        }
    }

    public boolean isConnected() {
        return connected;
    }
}

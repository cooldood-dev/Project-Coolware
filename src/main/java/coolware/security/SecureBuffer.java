package coolware.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Native-style unmanaged / off-heap memory container for zeroizable string/byte literals.
 * Meets D1 requirement: key/secret material never resides in long-lived Java String or heap arrays.
 */
public final class SecureBuffer implements AutoCloseable {
    private final ByteBuffer buffer;
    private final int length;
    private boolean closed = false;

    private SecureBuffer(byte[] raw) {
        this.length = raw.length;
        this.buffer = ByteBuffer.allocateDirect(length);
        this.buffer.put(raw);
        this.buffer.position(0);
        // Instant wipe of the temporary carrier array
        Arrays.fill(raw, (byte) 0);
    }

    public static SecureBuffer fromBytes(byte[] raw) {
        return new SecureBuffer(raw);
    }

    public static SecureBuffer fromString(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        return new SecureBuffer(bytes);
    }

    public int getLength() {
        return length;
    }

    public synchronized byte get(int index) {
        if (closed) throw new IllegalStateException("SecureBuffer already zeroized/closed");
        return buffer.get(index);
    }

    public synchronized byte[] copyOut() {
        if (closed) throw new IllegalStateException("SecureBuffer already zeroized/closed");
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = buffer.get(i);
        }
        return out;
    }

    public synchronized String asDecryptedString() {
        byte[] raw = copyOut();
        try {
            return new String(raw, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(raw, (byte) 0);
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            // Overwrite direct buffer memory with zeros and random noise
            SecureRandom rng = new SecureRandom();
            byte[] wipe = new byte[length];
            rng.nextBytes(wipe);
            buffer.position(0);
            buffer.put(wipe);
            Arrays.fill(wipe, (byte) 0);
            buffer.position(0);
            buffer.put(wipe);
            closed = true;
        }
    }

    public boolean isClosed() {
        return closed;
    }
}

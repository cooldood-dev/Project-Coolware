package coolware.security;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Enterprise Cryptographic Engine:
 * Implements HKDF-SHA256, AES-256-GCM authenticated encryption/decryption,
 * and Single-RTT Ratchet Heartbeat state engine.
 */
public final class SecurityEngine {
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    public enum Status {
        UNINITIALIZED,
        ACTIVE,
        LOCKED,
        TERMINATED
    }

    private static volatile Status currentStatus = Status.UNINITIALIZED;
    private static byte[] currentRatchetKey = new byte[32];
    private static long ratchetCounter = 0;
    private static final Object STATE_LOCK = new Object();

    public static Status getStatus() {
        return currentStatus;
    }

    public static void setStatus(Status status) {
        synchronized (STATE_LOCK) {
            currentStatus = status;
        }
    }

    public static boolean isExecutionAllowed() {
        return currentStatus == Status.ACTIVE;
    }

    /**
     * HKDF-Extract(salt, IKM) -> PRK
     */
    public static byte[] hkdfExtract(byte[] salt, byte[] ikm) {
        try {
            if (salt == null || salt.length == 0) {
                salt = new byte[32]; // 32 zeros
            }
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(salt, HMAC_ALGO));
            return mac.doFinal(ikm);
        } catch (Exception e) {
            throw new RuntimeException("HKDF-Extract failed", e);
        }
    }

    /**
     * HKDF-Expand(PRK, info, length) -> OKM
     */
    public static byte[] hkdfExpand(byte[] prk, byte[] info, int length) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(prk, HMAC_ALGO));
            byte[] okm = new byte[length];
            byte[] t = new byte[0];
            int offset = 0;
            byte counter = 1;

            while (offset < length) {
                mac.reset();
                mac.update(t);
                if (info != null) {
                    mac.update(info);
                }
                mac.update(counter);
                t = mac.doFinal();
                int toCopy = Math.min(t.length, length - offset);
                System.arraycopy(t, 0, okm, offset, toCopy);
                offset += toCopy;
                counter++;
            }
            return okm;
        } catch (Exception e) {
            throw new RuntimeException("HKDF-Expand failed", e);
        }
    }

    /**
     * Full HKDF: Extract then Expand
     */
    public static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) {
        byte[] prk = hkdfExtract(salt, ikm);
        return hkdfExpand(prk, info, length);
    }

    /**
     * AES-256-GCM Encrypt
     * Format: [12-byte IV][Ciphertext + 16-byte Tag]
     */
    public static byte[] encryptAesGcm(byte[] key, byte[] plaintext, byte[] aad) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), spec);
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return out;
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    /**
     * AES-256-GCM Decrypt
     */
    public static byte[] decryptAesGcm(byte[] key, byte[] blob, byte[] aad) {
        if (blob == null || blob.length < GCM_IV_LENGTH_BYTES + 16) {
            throw new IllegalArgumentException("Invalid AES-GCM blob length");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(blob, 0, iv, 0, GCM_IV_LENGTH_BYTES);

            int ctLen = blob.length - GCM_IV_LENGTH_BYTES;
            byte[] ciphertext = new byte[ctLen];
            System.arraycopy(blob, GCM_IV_LENGTH_BYTES, ciphertext, 0, ctLen);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), spec);
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            // Decryption or MAC authentication failure triggers locked state
            transitionToLocked("AES-GCM Auth Tag Mismatch or Tamper detected");
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }

    /**
     * Initializes the Ratchet Heartbeat state engine with master root key.
     */
    public static void initializeRatchet(byte[] rootKey) {
        synchronized (STATE_LOCK) {
            System.arraycopy(rootKey, 0, currentRatchetKey, 0, 32);
            ratchetCounter = 0;
            currentStatus = Status.ACTIVE;
        }
    }

    /**
     * Advances the single-RTT symmetric ratchet step.
     * key_{i+1} = HKDF-Expand(HKDF-Extract(key_i, counter), "ratchet_step", 32)
     */
    public static byte[] stepRatchet() {
        synchronized (STATE_LOCK) {
            if (currentStatus != Status.ACTIVE) {
                throw new IllegalStateException("Cannot advance ratchet when not ACTIVE");
            }
            ratchetCounter++;
            byte[] counterBytes = ByteBuffer.allocate(8).putLong(ratchetCounter).array();
            byte[] prk = hkdfExtract(currentRatchetKey, counterBytes);
            byte[] nextKey = hkdfExpand(prk, "ratchet_step".getBytes(StandardCharsets.UTF_8), 32);
            System.arraycopy(nextKey, 0, currentRatchetKey, 0, 32);
            return Arrays.copyOf(currentRatchetKey, 32);
        }
    }

    /**
     * Strict fail-closed transition to LOCKED state. Never terminates Minecraft process.
     */
    public static void transitionToLocked(String reason) {
        synchronized (STATE_LOCK) {
            currentStatus = Status.LOCKED;
            // Wipe volatile keys
            Arrays.fill(currentRatchetKey, (byte) 0);
            System.err.println("[Coolware-Security] INVARIANT TRIGGERED - Client locked: " + reason);
        }
    }
}

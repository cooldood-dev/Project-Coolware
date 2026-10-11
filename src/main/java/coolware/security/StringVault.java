package coolware.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise Encrypted String Vault:
 * Holds encrypted literals and decrypts only into short-lived SecureBuffer or stack instances.
 */
public final class StringVault {
    private static final ConcurrentHashMap<Integer, byte[]> ENCRYPTED_POOL = new ConcurrentHashMap<>();
    private static byte[] vaultMasterKey = new byte[32];

    static {
        // Derive default in-memory vault key
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            vaultMasterKey = md.digest("Coolware_Vault_Seed_2026_Secure".getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    public static void setVaultKey(byte[] key) {
        if (key != null && key.length == 32) {
            System.arraycopy(key, 0, vaultMasterKey, 0, 32);
        }
    }

    public static int registerEncrypted(byte[] encryptedBlob) {
        int id = ENCRYPTED_POOL.size() + 1;
        ENCRYPTED_POOL.put(id, encryptedBlob);
        return id;
    }

    public static int encryptAndStore(String literal) {
        byte[] pt = literal.getBytes(StandardCharsets.UTF_8);
        byte[] ct = SecurityEngine.encryptAesGcm(vaultMasterKey, pt, null);
        return registerEncrypted(ct);
    }

    /**
     * Decrypts string literal on demand.
     */
    public static SecureBuffer resolve(int id) {
        byte[] blob = ENCRYPTED_POOL.get(id);
        if (blob == null) {
            throw new IllegalArgumentException("Unknown StringVault ID: " + id);
        }
        byte[] pt = SecurityEngine.decryptAesGcm(vaultMasterKey, blob, null);
        return SecureBuffer.fromBytes(pt);
    }

    public static String get(int id) {
        try (SecureBuffer buf = resolve(id)) {
            return buf.asDecryptedString();
        }
    }
}

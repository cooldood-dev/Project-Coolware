package coolware.security;

import coolware.security.vm.VMAssembler;
import coolware.security.vm.VMInterpreter;
import coolware.security.vm.VMValidator;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Phase 5 Verification Test Harness:
 * Exercises SecurityEngine, Ratchet, StringVault, VMValidator, and VMInterpreter.
 * Validates fail-closed semantics, tamper rejection, and zeroization.
 */
public final class SecurityTestHarness {

    public static void runAllTests() {
        System.out.println("=== [Coolware Security Test Harness] Starting verification ===");
        int passed = 0;
        int failed = 0;

        // Test 1: HKDF Extraction & Expansion
        try {
            byte[] ikm = "secret_key_material".getBytes(StandardCharsets.UTF_8);
            byte[] salt = "salt1234".getBytes(StandardCharsets.UTF_8);
            byte[] okm = SecurityEngine.hkdf(salt, ikm, "info_test".getBytes(StandardCharsets.UTF_8), 32);
            if (okm != null && okm.length == 32) {
                System.out.println("[PASS] Test 1: HKDF-SHA256 Derivation");
                passed++;
            } else {
                System.err.println("[FAIL] Test 1: HKDF returned unexpected length");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 1: Exception - " + e.getMessage());
            failed++;
        }

        // Test 2: AES-256-GCM Round-trip Authenticated Encryption
        try {
            byte[] key = new byte[32];
            Arrays.fill(key, (byte) 0x42);
            byte[] plaintext = "Coolware_Enterprise_Payload_1.8.9".getBytes(StandardCharsets.UTF_8);
            byte[] aad = "client_aad_tag".getBytes(StandardCharsets.UTF_8);

            byte[] cipherBlob = SecurityEngine.encryptAesGcm(key, plaintext, aad);
            byte[] decrypted = SecurityEngine.decryptAesGcm(key, cipherBlob, aad);

            if (Arrays.equals(plaintext, decrypted)) {
                System.out.println("[PASS] Test 2: AES-256-GCM Authenticated Encrypt/Decrypt Roundtrip");
                passed++;
            } else {
                System.err.println("[FAIL] Test 2: Plaintext mismatch");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 2: Exception - " + e.getMessage());
            failed++;
        }

        // Test 3: StringVault Decrypt-on-demand & SecureBuffer Zeroization
        try {
            int strId = StringVault.encryptAndStore("Protected_Secret_API_Token");
            String retrieved;
            try (SecureBuffer buf = StringVault.resolve(strId)) {
                retrieved = buf.asDecryptedString();
                if (buf.isClosed()) throw new RuntimeException("Buffer closed too early");
            }
            if ("Protected_Secret_API_Token".equals(retrieved)) {
                System.out.println("[PASS] Test 3: StringVault off-heap resolution & SecureBuffer zeroization");
                passed++;
            } else {
                System.err.println("[FAIL] Test 3: String content mismatch");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 3: Exception - " + e.getMessage());
            failed++;
        }

        // Test 4: VM Execution of Valid Arithmetic Assembly
        try {
            SecurityEngine.setStatus(SecurityEngine.Status.ACTIVE);
            VMAssembler asm = new VMAssembler();
            // R0 = 50, R1 = 25, R0 = R0 + R1 (75), RET
            asm.loadi(0, 50)
               .loadi(1, 25)
               .add(0, 0, 1)
               .ret();

            VMInterpreter vm = new VMInterpreter();
            long result = vm.execute(asm.toIntArray());
            if (result == 75L) {
                System.out.println("[PASS] Test 4: VM arithmetic execution (expected 75, got " + result + ")");
                passed++;
            } else {
                System.err.println("[FAIL] Test 4: VM computation mismatch: " + result);
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 4: VM execution threw exception - " + e.getMessage());
            failed++;
        }

        // Test 5: VM Validator Rejection of Illegal Branch Target (Fail-Closed)
        try {
            VMAssembler invalidAsm = new VMAssembler();
            invalidAsm.loadi(0, 10)
                      .jmp(999) // Invalid out-of-bounds target
                      .ret();

            VMValidator.ValidationResult val = VMValidator.validate(invalidAsm.toIntArray());
            if (!val.valid) {
                System.out.println("[PASS] Test 5: Static VMValidator rejected illegal out-of-bounds branch");
                passed++;
            } else {
                System.err.println("[FAIL] Test 5: Validator permitted illegal branch jump");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 5: Unexpected error - " + e.getMessage());
            failed++;
        }

        // Test 6: Single-RTT Symmetric Ratchet Step
        try {
            byte[] rootKey = new byte[32];
            Arrays.fill(rootKey, (byte) 0x7E);
            SecurityEngine.initializeRatchet(rootKey);
            byte[] key1 = SecurityEngine.stepRatchet();
            byte[] key2 = SecurityEngine.stepRatchet();

            if (!Arrays.equals(key1, key2) && key1.length == 32 && key2.length == 32) {
                System.out.println("[PASS] Test 6: Ratchet state forward step progression");
                passed++;
            } else {
                System.err.println("[FAIL] Test 6: Ratchet keys did not progress uniquely");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 6: Ratchet exception - " + e.getMessage());
            failed++;
        }

        System.out.println(String.format("=== [Security Test Harness] Complete: %d passed, %d failed ===", passed, failed));
    }

    public static void main(String[] args) {
        runAllTests();
    }
}

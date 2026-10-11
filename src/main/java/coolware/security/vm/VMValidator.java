package coolware.security.vm;

import java.util.BitSet;

/**
 * Static Bytecode Pre-Execution Validator (Phase 3 Spec):
 * Validates .vmbc instruction streams prior to execution.
 * Checks opcode ranges, register boundaries (0..15), jump targets, and basic block integrity.
 */
public final class VMValidator {
    public static final int MAX_INSTRUCTIONS = 65536;
    public static final int NUM_REGISTERS = 16;

    // Supported Opcodes
    public static final int OP_NOP      = 0x00;
    public static final int OP_MOV      = 0x01;
    public static final int OP_LOADI    = 0x02;
    public static final int OP_ADD      = 0x03;
    public static final int OP_SUB      = 0x04;
    public static final int OP_MUL      = 0x05;
    public static final int OP_DIV      = 0x06;
    public static final int OP_AND      = 0x07;
    public static final int OP_OR       = 0x08;
    public static final int OP_XOR      = 0x09;
    public static final int OP_SHL      = 0x0A;
    public static final int OP_SHR      = 0x0B;
    public static final int OP_CMP      = 0x0C;
    public static final int OP_JMP      = 0x0D;
    public static final int OP_JZ       = 0x0E;
    public static final int OP_JNZ      = 0x0F;
    public static final int OP_CALL     = 0x10;
    public static final int OP_RET      = 0x11;
    public static final int OP_HALT     = 0x12;

    public static class ValidationResult {
        public final boolean valid;
        public final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String msg) {
            return new ValidationResult(false, msg);
        }
    }

    public static ValidationResult validate(int[] code) {
        if (code == null || code.length == 0) {
            return ValidationResult.error("Bytecode stream is empty or null");
        }
        if (code.length > MAX_INSTRUCTIONS) {
            return ValidationResult.error("Bytecode exceeds MAX_INSTRUCTIONS limit (" + code.length + ")");
        }

        BitSet validJumpTargets = new BitSet(code.length);
        for (int i = 0; i < code.length; i++) {
            validJumpTargets.set(i);
        }

        boolean hasTerminator = false;

        for (int pc = 0; pc < code.length; pc++) {
            int word = code[pc];
            int opcode = (word >>> 24) & 0xFF;
            int rD = (word >>> 20) & 0x0F;
            int rA = (word >>> 16) & 0x0F;
            int rB = (word >>> 12) & 0x0F;
            int imm16 = word & 0xFFFF;
            int signedImm16 = (short) imm16;

            if (rD >= NUM_REGISTERS || rA >= NUM_REGISTERS || rB >= NUM_REGISTERS) {
                return ValidationResult.error("Register index out of bounds at PC " + pc);
            }

            switch (opcode) {
                case OP_NOP:
                case OP_MOV:
                case OP_LOADI:
                case OP_ADD:
                case OP_SUB:
                case OP_MUL:
                case OP_DIV:
                case OP_AND:
                case OP_OR:
                case OP_XOR:
                case OP_SHL:
                case OP_SHR:
                case OP_CMP:
                case OP_CALL:
                    break;

                case OP_JMP:
                case OP_JZ:
                case OP_JNZ: {
                    int targetPc = pc + signedImm16;
                    if (targetPc < 0 || targetPc >= code.length) {
                        return ValidationResult.error("Branch target out of bounds at PC " + pc + " -> target " + targetPc);
                    }
                    if (opcode == OP_JMP && pc == code.length - 1) {
                        hasTerminator = true;
                    }
                    break;
                }

                case OP_RET:
                case OP_HALT:
                    if (pc == code.length - 1) {
                        hasTerminator = true;
                    }
                    break;

                default:
                    return ValidationResult.error("Unknown or illegal opcode 0x" + Integer.toHexString(opcode) + " at PC " + pc);
            }
        }

        if (!hasTerminator) {
            return ValidationResult.error("Bytecode lacks terminal instruction (RET / HALT / JMP) at stream end");
        }

        return ValidationResult.ok();
    }
}

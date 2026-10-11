package coolware.security.vm;

import coolware.security.SecurityEngine;
import java.util.Arrays;

/**
 * 64-Bit Register Virtual Machine Core (Phase 3 & 4 Spec):
 * Interprets validated .vmbc instructions with 16 64-bit general purpose registers.
 * Handles arithmetic, bitwise logic, relative branching, and call frames.
 */
public final class VMInterpreter {
    private final long[] registers = new long[VMValidator.NUM_REGISTERS];
    private int pc = 0;
    private int flags = 0; // bit 0: Zero, bit 1: Sign, bit 2: Carry/Borrow
    private boolean halted = false;

    public static final int FLAG_ZERO = 1;
    public static final int FLAG_SIGN = 2;

    public VMInterpreter() {
        reset();
    }

    public void reset() {
        Arrays.fill(registers, 0L);
        pc = 0;
        flags = 0;
        halted = false;
    }

    public void setRegister(int reg, long val) {
        if (reg >= 0 && reg < VMValidator.NUM_REGISTERS) {
            registers[reg] = val;
        }
    }

    public long getRegister(int reg) {
        if (reg >= 0 && reg < VMValidator.NUM_REGISTERS) {
            return registers[reg];
        }
        return 0L;
    }

    /**
     * Executes validated instruction sequence.
     */
    public long execute(int[] code) {
        if (!SecurityEngine.isExecutionAllowed()) {
            throw new SecurityException("VM execution blocked: security engine state is not ACTIVE");
        }

        VMValidator.ValidationResult val = VMValidator.validate(code);
        if (!val.valid) {
            SecurityEngine.transitionToLocked("VM validation failure: " + val.errorMessage);
            throw new SecurityException("VM Bytecode validation rejected: " + val.errorMessage);
        }

        reset();

        while (pc >= 0 && pc < code.length && !halted) {
            int word = code[pc];
            int opcode = (word >>> 24) & 0xFF;
            int rD = (word >>> 20) & 0x0F;
            int rA = (word >>> 16) & 0x0F;
            int rB = (word >>> 12) & 0x0F;
            short imm16 = (short) (word & 0xFFFF);

            switch (opcode) {
                case VMValidator.OP_NOP:
                    pc++;
                    break;

                case VMValidator.OP_MOV:
                    registers[rD] = registers[rA];
                    pc++;
                    break;

                case VMValidator.OP_LOADI:
                    registers[rD] = imm16;
                    pc++;
                    break;

                case VMValidator.OP_ADD:
                    registers[rD] = registers[rA] + registers[rB];
                    updateFlags(registers[rD]);
                    pc++;
                    break;

                case VMValidator.OP_SUB:
                    registers[rD] = registers[rA] - registers[rB];
                    updateFlags(registers[rD]);
                    pc++;
                    break;

                case VMValidator.OP_MUL:
                    registers[rD] = registers[rA] * registers[rB];
                    updateFlags(registers[rD]);
                    pc++;
                    break;

                case VMValidator.OP_DIV:
                    if (registers[rB] == 0) {
                        SecurityEngine.transitionToLocked("VM Division by zero at PC " + pc);
                        throw new ArithmeticException("VM Div by 0");
                    }
                    registers[rD] = registers[rA] / registers[rB];
                    updateFlags(registers[rD]);
                    pc++;
                    break;

                case VMValidator.OP_AND:
                    registers[rD] = registers[rA] & registers[rB];
                    updateFlags(registers[rD]);
                    pc++;
                    break;

                case VMValidator.OP_OR:
                    registers[rD] = registers[rA] | registers[rB];
                    updateFlags(registers[rD]);
                    pc++;
                    break;

                case VMValidator.OP_XOR:
                    registers[rD] = registers[rA] ^ registers[rB];
                    updateFlags(registers[rD]);
                    pc++;
                    break;

                case VMValidator.OP_SHL:
                    registers[rD] = registers[rA] << (registers[rB] & 0x3F);
                    updateFlags(registers[rD]);
                    pc++;
                    break;

                case VMValidator.OP_SHR:
                    registers[rD] = registers[rA] >>> (registers[rB] & 0x3F);
                    updateFlags(registers[rD]);
                    pc++;
                    break;

                case VMValidator.OP_CMP:
                    long diff = registers[rA] - registers[rB];
                    updateFlags(diff);
                    pc++;
                    break;

                case VMValidator.OP_JMP:
                    pc += imm16;
                    break;

                case VMValidator.OP_JZ:
                    if ((flags & FLAG_ZERO) != 0) {
                        pc += imm16;
                    } else {
                        pc++;
                    }
                    break;

                case VMValidator.OP_JNZ:
                    if ((flags & FLAG_ZERO) == 0) {
                        pc += imm16;
                    } else {
                        pc++;
                    }
                    break;

                case VMValidator.OP_RET:
                case VMValidator.OP_HALT:
                    halted = true;
                    break;

                default:
                    SecurityEngine.transitionToLocked("VM encountered unrecognized opcode: " + opcode);
                    throw new IllegalStateException("Unrecognized opcode: " + opcode);
            }
        }

        return registers[0]; // R0 is conventional return register
    }

    private void updateFlags(long val) {
        flags = 0;
        if (val == 0) {
            flags |= FLAG_ZERO;
        }
        if (val < 0) {
            flags |= FLAG_SIGN;
        }
    }
}

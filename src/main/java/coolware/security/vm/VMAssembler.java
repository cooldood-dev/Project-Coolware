package coolware.security.vm;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembler for .vmbc binary instruction format.
 * Emits validated 32-bit machine words and encodes them into binary streams.
 */
public final class VMAssembler {
    private final List<Integer> instructions = new ArrayList<>();

    public VMAssembler emit(int opcode, int rD, int rA, int rB, int imm16) {
        int word = ((opcode & 0xFF) << 24)
                 | ((rD & 0x0F) << 20)
                 | ((rA & 0x0F) << 16)
                 | ((rB & 0x0F) << 12)
                 | (imm16 & 0xFFFF);
        instructions.add(word);
        return this;
    }

    public VMAssembler loadi(int rD, int imm16) {
        return emit(VMValidator.OP_LOADI, rD, 0, 0, imm16);
    }

    public VMAssembler mov(int rD, int rA) {
        return emit(VMValidator.OP_MOV, rD, rA, 0, 0);
    }

    public VMAssembler add(int rD, int rA, int rB) {
        return emit(VMValidator.OP_ADD, rD, rA, rB, 0);
    }

    public VMAssembler sub(int rD, int rA, int rB) {
        return emit(VMValidator.OP_SUB, rD, rA, rB, 0);
    }

    public VMAssembler mul(int rD, int rA, int rB) {
        return emit(VMValidator.OP_MUL, rD, rA, rB, 0);
    }

    public VMAssembler div(int rD, int rA, int rB) {
        return emit(VMValidator.OP_DIV, rD, rA, rB, 0);
    }

    public VMAssembler xor(int rD, int rA, int rB) {
        return emit(VMValidator.OP_XOR, rD, rA, rB, 0);
    }

    public VMAssembler cmp(int rA, int rB) {
        return emit(VMValidator.OP_CMP, 0, rA, rB, 0);
    }

    public VMAssembler jmp(int relativeOffset) {
        return emit(VMValidator.OP_JMP, 0, 0, 0, relativeOffset);
    }

    public VMAssembler jz(int relativeOffset) {
        return emit(VMValidator.OP_JZ, 0, 0, 0, relativeOffset);
    }

    public VMAssembler jnz(int relativeOffset) {
        return emit(VMValidator.OP_JNZ, 0, 0, 0, relativeOffset);
    }

    public VMAssembler ret() {
        return emit(VMValidator.OP_RET, 0, 0, 0, 0);
    }

    public VMAssembler halt() {
        return emit(VMValidator.OP_HALT, 0, 0, 0, 0);
    }

    public int[] toIntArray() {
        int[] arr = new int[instructions.size()];
        for (int i = 0; i < instructions.size(); i++) {
            arr[i] = instructions.get(i);
        }
        return arr;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeInt(0x564D4243); // Magic "VMBC"
            dos.writeInt(instructions.size());
            for (int word : instructions) {
                dos.writeInt(word);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize .vmbc", e);
        }
    }
}

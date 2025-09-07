package testes;

import java.io.IOException;

import com.emulator.Cartridge;

public class DisassemblerCPU68000Advanced {
	
	public static String generateDisassembly(Cartridge cart) throws IOException {
        byte[] rom = cart.getROMData();
        StringBuilder sb = new StringBuilder();
        int pc = 0;

        while (pc < rom.length) {
            if (pc + 1 >= rom.length) break; // evita ler além do final
            int opcode = ((rom[pc] & 0xFF) << 8) | (rom[pc + 1] & 0xFF);
            String instr = decodeInstruction(opcode, pc);
            sb.append(String.format("%06X: %04X  %s\n", pc, opcode, instr));
            pc += 2;
        }
        return sb.toString();
    }

    private static String decodeInstruction(int opcode, int pc) {
        int hi = (opcode & 0xF000) >> 12;
        int lowByte = opcode & 0xFF;

        switch (hi) {
            case 0x1: return "MOVE";
            case 0x2: return "MOVE";
            case 0x3: return "MOVE";
            case 0x4: return "NEG/CLR";
            case 0x5: return "ADD";
            case 0x6: {
                int offset = lowByte;
                if ((offset & 0x80) != 0) offset -= 0x100;
                int target = pc + 2 + offset;
                return String.format("BNE %06X", target);
            }
            case 0x7: return "MOVEQ";
            case 0x8: return "JSR/BSR";
            case 0xB: return "CMP";
            case 0xC: return "AND/OR";
            case 0xD: return "ADDQ/SUBQ";
            default: return "???";
        }
    }

}

package testes;

import emulator02.Cartridge;

public class DisassemblerCPU68000Advanced01 {	
	
	public static String generateDisassembly(Cartridge cart) {
		byte[] rom = cart.getROMData();
        StringBuilder sb = new StringBuilder();
        int pc = 0;

        while (pc < rom.length) {
            // Se restar apenas 1 byte no fim da ROM
            if (pc + 1 >= rom.length) {
                sb.append(String.format("%06X: %02X       ??? (byte solto)\n", pc, rom[pc] & 0xFF));
                break;
            }

            int opcode = ((rom[pc] & 0xFF) << 8) | (rom[pc + 1] & 0xFF);
            String instr = decodeInstruction(opcode, pc);

            sb.append(String.format("%06X: %04X  %s\n", pc, opcode, instr));

            pc += 2; // avança 2 bytes por instrução (simples didático)
        }

        return sb.toString();
    }

    private static String decodeInstruction(int opcode, int pc) {
        // Didático: apenas alguns opcodes reais do 68k
        switch (opcode & 0xF000) {
            case 0x1000: return "MOVE";     // MOVE
            case 0x2000: return "MOVEA";    // MOVEA
            case 0x3000: return "MOVE";     // MOVE
            case 0x4000: return "NEG/CLR";  // NEG, CLR, etc.
            case 0x5000: return "ADDQ/SUBQ";// ADDQ/SUBQ
            case 0x6000: return "BRA/BSR";  // Branch
            case 0x7000: return "MOVEQ";    // MOVE Quick
            case 0x8000: return "OR";       // OR
            case 0x9000: return "SUB";      // SUB
            case 0xB000: return "CMP";      // CMP
            case 0xC000: return "AND";      // AND
            case 0xD000: return "ADD";      // ADD
            case 0xE000: return "SHIFT";    // Shifts/rotates
        }

        // Alguns opcodes específicos
        if (opcode == 0x4E71) return "NOP";
        if (opcode == 0x4E75) return "RTS";
        if (opcode == 0x4E72) return "STOP";
        if (opcode == 0x4E73) return "RTE";
        if (opcode == 0x4E70) return "RESET";
        if (opcode == 0x4E74) return "RTD";

        return "???";
    }

}

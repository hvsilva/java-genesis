package emulator03;

public class Disassembler68000 {	
	

	public static String generateDisassembly(Cartridge cart) {
		byte[] rom = cart.getROMData();
        StringBuilder sb = new StringBuilder();
        int pc = 0;

        while (pc < rom.length) {
            if (pc + 1 >= rom.length) {
                sb.append(String.format("%06X: %02X       ??? (byte solto)\n", pc, rom[pc] & 0xFF));
                break;
            }

            int opcode = ((rom[pc] & 0xFF) << 8) | (rom[pc + 1] & 0xFF);
            int instrStart = pc;
            pc += 2;

            String instr = decodeInstruction(opcode, instrStart, rom, new int[]{pc});
            pc = instr.endsWith("...") ? rom.length : (instr.contains("##NEXT##") ? Integer.parseInt(instr.split("##NEXT##")[1]) : pc);
            instr = instr.replaceAll("##NEXT##\\d+", "");

            sb.append(String.format("%06X: %04X  %s\n", instrStart, opcode, instr));
        }

        return sb.toString();
    }

	public  static String decodeInstruction(int opcode, int pc, byte[] rom, int[] nextPc) {
        // MOVEQ (0x7000–0x70FF)
        if ((opcode & 0xF100) == 0x7000) {
            int reg = (opcode >> 9) & 0x7;
            int imm = (byte)(opcode & 0xFF);
            return String.format("MOVEQ #%d, D%d", imm, reg);
        }

        // BRA (0x6000)
        if ((opcode & 0xFF00) == 0x6000) {
            int offset = (byte)(opcode & 0xFF);
            int target = pc + offset;
            return String.format("BRA $%06X", target & 0xFFFFFF);
        }

        // BSR (0x6100)
        if ((opcode & 0xFF00) == 0x6100) {
            int offset = (byte)(opcode & 0xFF);
            int target = pc + offset;
            return String.format("BSR $%06X", target & 0xFFFFFF);
        }

        // MOVE imediato para Dn (exemplo simplificado: 0x303C = MOVE.W #imm, D0)
        if ((opcode & 0xFFF8) == 0x303C) {
            int reg = opcode & 0x7;
            if (pc + 1 >= rom.length) return "MOVE.W ??? (incompleto)";
            int imm = ((rom[pc] & 0xFF) << 8) | (rom[pc + 1] & 0xFF);
            nextPc[0] = pc + 2;
            return String.format("MOVE.W #$%04X, D%d##NEXT##%d", imm, reg, nextPc[0]);
        }

        // MOVE.L imediato (0x203C = MOVE.L #imm32, D0)
        if ((opcode & 0xFFF8) == 0x203C) {
            int reg = opcode & 0x7;
            if (pc + 3 >= rom.length) return "MOVE.L ??? (incompleto)";
            int imm = ((rom[pc] & 0xFF) << 24) | ((rom[pc + 1] & 0xFF) << 16) |
                      ((rom[pc + 2] & 0xFF) << 8) | (rom[pc + 3] & 0xFF);
            nextPc[0] = pc + 4;
            return String.format("MOVE.L #$%08X, D%d##NEXT##%d", imm, reg, nextPc[0]);
        }

        // ADD (simplificado)
        if ((opcode & 0xF000) == 0xD000) {
            int reg = opcode & 0x7;
            return String.format("ADD ???, D%d", reg);
        }

        // SUB (simplificado)
        if ((opcode & 0xF000) == 0x9000) {
            int reg = opcode & 0x7;
            return String.format("SUB ???, D%d", reg);
        }

        // Instruções de controle
        if (opcode == 0x4E71) return "NOP";
        if (opcode == 0x4E75) return "RTS";
        if (opcode == 0x4E72) return "STOP";
        if (opcode == 0x4E70) return "RESET";

        return "???";
     }

}

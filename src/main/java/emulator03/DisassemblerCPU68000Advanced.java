package emulator03;

public class DisassemblerCPU68000Advanced {	
	
	/** Gera disassembly simples da ROM (apenas para listagem inicial) */
	public static String generateDisassembly(Cartridge cart) {
		StringBuilder sb = new StringBuilder();
		byte[] rom = cart.getROMData();

		int pc = 0;
		for (int i = 0; i < 200 && pc < rom.length - 1; i++) {
			int opcode = ((rom[pc] & 0xFF) << 8) | (rom[pc + 1] & 0xFF);
			String instr = decode(opcode);

			sb.append(String.format("%06X: %04X  %-10s%n", pc, opcode, instr));
			pc += 2;
		}
		return sb.toString();
	}

	/** Dump do estado atual da CPU (registradores + PC) */
	public static String dumpCPUState(CPU68000 cpu) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("PC=%04X | ", cpu.getPC()));

		int[] regs = cpu.getRegisters();
		for (int i = 0; i < 8; i++) {
			sb.append(String.format("D%d=%04X ", i, regs[i]));
		}
		for (int i = 8; i < 16; i++) {
			sb.append(String.format("A%d=%04X ", i - 8, regs[i]));
		}
		return sb.toString();
	}

	/** Decodificação didática */
	public static String decode(int opcode) {
		int top = opcode & 0xF000;

		switch (top) {
		case 0x1000:
			return "MOVE";
		case 0x2000:
			return "ADD";
		case 0x3000:
			return "SUB";
		case 0x4000:
			return "JMP";
		case 0x5000:
			return "CMP";
		case 0x6000:
			return "BRA/BSR/Bcc";
		case 0x7000:
			return "MOVEQ";
		case 0x8000:
			return "OR";
		case 0x9000:
			return "SUB";
		case 0xB000:
			return "CMP";
		case 0xC000:
			return "AND";
		case 0xD000:
			return "ADD";
		case 0xE000:
			return "SHIFT/ROT";
		}

		// Especiais
		if ((opcode & 0xFFF8) == 0x4E70) {
			switch (opcode & 0xFF) {
			case 0x71:
				return "NOP";
			case 0x75:
				return "RTS";
			case 0x77:
				return "RTR";
			case 0x73:
				return "RTE";
			}
		}
		if ((opcode & 0xFFC0) == 0x4EC0)
			return "JMP";
		if ((opcode & 0xFFC0) == 0x4E80)
			return "JSR";

		return "???";
	}

}

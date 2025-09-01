package testes;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DisassemblerCPU68000 {
	private byte[] rom;
	private Set<Integer> branchTargets = new HashSet<>();
	

	public static void main(String[] args) {
		try {
			DisassemblerCPU68000 dis = new DisassemblerCPU68000("D:\\\\PROJETOS_EMULADOR\\\\java-genesis\\\\roms\\\\SONIC1.BIN");
			dis.disassemble();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DisassemblerCPU68000(String romPath) throws IOException {
	        File file = new File(romPath);
	        rom = new byte[(int) file.length()];
	        try (FileInputStream fis = new FileInputStream(file)) {
	            fis.read(rom);
	        }
	    }

	public void disassemble() {
		System.out.println("ROM size: " + rom.length + " bytes\n");
		int pc = 0;

		while (pc < rom.length) {
			int opcode = ((rom[pc] & 0xFF) << 8) | (rom[pc + 1] & 0xFF);
			String instr = decodeInstruction(opcode, pc);
			System.out.printf("%06X: %04X  %s\n", pc, opcode, instr);
			pc += 2;
		}
	}

	private String decodeInstruction(int opcode, int pc) {
		int hi = (opcode & 0xF000) >> 12;

		switch (hi) {
		case 0x0:
			return "BRANCH / NOP / ???";
		case 0x1:
			return "MOVE"; // Exemplo simplificado
		case 0x2:
			return "MOVE";
		case 0x3:
			return "MOVE";
		case 0x4:
			return "NEG/CLR";
		case 0x5:
			return "ADD";
		case 0x6: {
			int offset = opcode & 0xFF;
			int target = pc + 2 + (offset & 0x80) != 0 ? offset - 0x100 : offset;
			branchTargets.add(target);
			return "BNE " + String.format("%06X", target);
		}
		case 0x7:
			return "MOVEQ";
		case 0x8:
			return "JSR/BSR";
		case 0xC:
			return "AND/OR";
		default:
			return "???";
		}
	}
}

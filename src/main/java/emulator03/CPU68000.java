package emulator03;

public class CPU68000 {

	private int[] registers = new int[16]; // D0-D7 (0-7), A0-A7 (8-15)
	private int pc; // Program Counter
	private Memory memory;

	// Status Register simplificado
	private boolean flagZ; // Zero
	private boolean flagN; // Negative
	private boolean flagC; // Carry
	private boolean flagV; // Overflow

	// Pilha simples em A7 (SP)
	private int getSP() {
		return registers[15];
	}

	private void setSP(int value) {
		registers[15] = value;
	}

	public CPU68000(Memory mem) {
		this.memory = mem;
		this.pc = 0; // endereço inicial
		setSP(0xFFFE); // stack no topo da RAM (exemplo)
	}

	public void step() {
		int opcode = memory.readWord(pc);
		pc += 2;
		decodeAndExecute(opcode);
		dumpState(opcode);
	}

	private void decodeAndExecute(int opcode) {
		switch (opcode & 0xF000) {
		case 0x1000:
			executeMOVE(opcode);
			break;
		case 0x2000:
			executeADD(opcode);
			break;
		case 0x3000:
			executeSUB(opcode);
			break;
		case 0x4000:
			executeJMP(opcode);
			break;
		case 0x5000:
			executeCMP(opcode);
			break;
		case 0x6000:
			executeBRA(opcode);
			break;
		case 0x4E80:
			executeJSR(opcode);
			break;
		case 0x4E75:
			executeRTS();
			break;
		default:
			System.out.printf("Opcode não implementado: %04X%n", opcode);
		}
	}

	/** MOVE #imediato, Dx */
	private void executeMOVE(int opcode) {
		int reg = opcode & 0x0007;
		int value = memory.readWord(pc);
		pc += 2;
		registers[reg] = value & 0xFFFF;
		updateFlags(value);
		System.out.printf("MOVE #%d -> D%d%n", value, reg);
	}

	/** ADD #imediato, Dx */
	private void executeADD(int opcode) {
		int reg = opcode & 0x0007;
		int value = memory.readWord(pc);
		pc += 2;

		int result = registers[reg] + value;
		updateFlags(result);

		flagC = (result & 0x10000) != 0;
		flagV = ((registers[reg] ^ value) >= 0) && ((registers[reg] ^ result) < 0);

		registers[reg] = result & 0xFFFF;

		System.out.printf("ADD #%d -> D%d (novo valor: %d)%n", value, reg, registers[reg]);
	}

	/** SUB #imediato, Dx */
	private void executeSUB(int opcode) {
		int reg = opcode & 0x0007;
		int value = memory.readWord(pc);
		pc += 2;

		int result = registers[reg] - value;
		updateFlags(result);

		flagC = (result < 0);
		flagV = ((registers[reg] ^ value) < 0) && ((registers[reg] ^ result) < 0);

		registers[reg] = result & 0xFFFF;

		System.out.printf("SUB #%d -> D%d (novo valor: %d)%n", value, reg, registers[reg]);
	}

	/** CMP #imediato, Dx */
	private void executeCMP(int opcode) {
		int reg = opcode & 0x0007;
		int value = memory.readWord(pc);
		pc += 2;

		int result = registers[reg] - value;
		updateFlags(result);

		flagC = (result < 0);
		flagV = ((registers[reg] ^ value) < 0) && ((registers[reg] ^ result) < 0);

		System.out.printf("CMP #%d, D%d -> resultado=%d%n", value, reg, result);
	}

	/** BRA deslocamento */
	private void executeBRA(int opcode) {
		int disp = opcode & 0xFF;
		if (disp == 0) {
			disp = memory.readWord(pc);
			pc += 2;
		}
		if ((disp & 0x80) != 0)
			disp |= 0xFFFFFF00; // sign extend

		pc += disp;
		System.out.printf("BRA %04X%n", pc);
	}

	/** JSR endereço absoluto */
	private void executeJSR(int opcode) {
		int addr = memory.readWord(pc);
		pc += 2;
		// empilha PC atual
		setSP(getSP() - 2);
		memory.writeWord(getSP(), pc);
		pc = addr;
		System.out.printf("JSR %04X%n", addr);
	}

	/** RTS */
	private void executeRTS() {
		int addr = memory.readWord(getSP());
		setSP(getSP() + 2);
		pc = addr;
		System.out.println("RTS");
	}

	/** JMP endereço absoluto */
	private void executeJMP(int opcode) {
		int addr = memory.readWord(pc);
		pc = addr;
		System.out.printf("JMP %04X%n", addr);
	}

	/** Atualiza flags Z e N */
	private void updateFlags(int result) {
		flagZ = (result & 0xFFFF) == 0;
		flagN = (result & 0x8000) != 0;
	}

	/** Debug: dump do estado da CPU */
	private void dumpState(int opcode) {
		System.out.printf("PC=%04X  OPCODE=%04X | ", pc, opcode);
		for (int i = 0; i < 8; i++) {
			System.out.printf("D%d=%04X ", i, registers[i]);
		}
		System.out.printf("| Flags [Z=%b N=%b C=%b V=%b]%n", flagZ, flagN, flagC, flagV);
	}
	
	public String stepWithDisasm() {
		int opcode = memory.readWord(pc);
	    int currentPC = pc;
	    pc += 2;

	    // Cria um nextPc "falso" para reaproveitar o decode do disassembler
	    int[] nextPc = new int[] { pc };
	    byte[] fakeRom = memory.getRawData(); // precisa expor isso em Memory
	    String instrText = Disassembler68000.decodeInstruction(opcode, currentPC, fakeRom, nextPc);

	    // Ajusta PC se decode avançou
	    pc = nextPc[0];

	    // Executa a instrução real
	    decodeAndExecute(opcode);

	    return String.format("%06X: %04X  %s", currentPC, opcode, instrText.replaceAll("##NEXT##\\d+", ""));
	}

	// Getters
	public int getPC() {
		return pc;
	}

	public int[] getRegisters() {
		return registers;
	}
}

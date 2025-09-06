package emulator02;

public class CPU68000 {	
	private int[] registers = new int[16]; // Registradores: D0-D7 = 0-7, A0-A7 = 8-15
	private int pc; // Program Counter
	private Memory memory;

	// Flags (simplificadas)
	private boolean flagZ; // Zero
	private boolean flagN; // Negative
	private boolean flagC; // Carry
	private boolean flagV; // Overflow

	public CPU68000(Memory mem) {
		this.memory = mem;
		this.pc = 0;
		setSP(0xFFFE); // topo da pilha
	}

	// Step: executa uma instrução
	public void step() {
		int opcode = memory.readWord(pc);
		pc += 2;
		executeInstruction(opcode);
		dumpState(opcode);
	}

	// Executa a instrução baseada no opcode
	public void executeInstruction(int opcode) {
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
		case 0x5000:
			executeCMP(opcode);
			break;
		case 0x6000:
			executeBRA(opcode);
			break;
		}

		// Grupos específicos
		if ((opcode & 0xFFC0) == 0x4EC0)
			executeJMP(opcode);
		if ((opcode & 0xFFFF) == 0x4E75)
			executeRTS();
		if ((opcode & 0xFFFF) == 0x4E80)
			executeJSR(opcode);
	}

	/** MOVE #imediato, Dx */
	private void executeMOVE(int opcode) {
		int reg = opcode & 0x0007;
		int value = memory.readWord(pc);
		pc += 2;
		setD(reg, value);
		updateFlags(value);
		System.out.printf("MOVE #%d -> D%d%n", value, reg);
	}

	/** ADD #imediato, Dx */
	private void executeADD(int opcode) {
		int reg = opcode & 0x0007;
		int value = memory.readWord(pc);
		pc += 2;
		int result = getD(reg) + value;
		setD(reg, result);
		flagC = (result & 0x10000) != 0;
		flagV = ((getD(reg) ^ value) >= 0) && ((getD(reg) ^ result) < 0);
		updateFlags(result);
		System.out.printf("ADD #%d -> D%d (novo valor: %d)%n", value, reg, getD(reg));
	}

	/** SUB #imediato, Dx */
	private void executeSUB(int opcode) {
		int reg = opcode & 0x0007;
		int value = memory.readWord(pc);
		pc += 2;
		int result = getD(reg) - value;
		setD(reg, result);
		flagC = (result < 0);
		flagV = ((getD(reg) ^ value) < 0) && ((getD(reg) ^ result) < 0);
		updateFlags(result);
		System.out.printf("SUB #%d -> D%d (novo valor: %d)%n", value, reg, getD(reg));
	}

	/** CMP #imediato, Dx */
	private void executeCMP(int opcode) {
		int reg = opcode & 0x0007;
		int value = memory.readWord(pc);
		pc += 2;
		int result = getD(reg) - value;
		updateFlags(result);
		flagC = (result < 0);
		flagV = ((getD(reg) ^ value) < 0) && ((getD(reg) ^ result) < 0);
		System.out.printf("CMP #%d, D%d -> resultado=%d%n", value, reg, result);
	}

	/** BRA relativo */
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

	/** JMP absoluto */
	private void executeJMP(int opcode) {
		int addr = memory.readWord(pc);
		pc = addr;
		System.out.printf("JMP %04X%n", addr);
	}

	/** JSR absoluto */
	private void executeJSR(int opcode) {
		int addr = memory.readWord(pc);
		pc += 2;
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

	/** Atualiza flags Z/N */
	private void updateFlags(int result) {
		flagZ = (result & 0xFFFF) == 0;
		flagN = (result & 0x8000) != 0;
	}

	/** Dump completo para debug */
	private void dumpState(int opcode) {
		System.out.printf("PC=%04X  OPCODE=%04X%n", pc, opcode);
		for (int i = 0; i < 8; i++)
			System.out.printf("D%d=%04X ", i, getD(i));
		System.out.println();
		for (int i = 0; i < 8; i++)
			System.out.printf("A%d=%04X ", i, getA(i));
		System.out.printf("SP=%04X%n", getSP());
		System.out.printf("Flags [Z=%b N=%b C=%b V=%b]%n", flagZ, flagN, flagC, flagV);
	}

	// Getters e setters para registradores D e A
	public int getD(int i) {
		return registers[i] & 0xFFFF;
	}

	public void setD(int i, int value) {
		registers[i] = value & 0xFFFF;
	}

	public int getA(int i) {
		return registers[8 + i] & 0xFFFF;
	}

	public void setA(int i, int value) {
		registers[8 + i] = value & 0xFFFF;
	}

	public int getPC() {
		return pc;
	}

	public void setPC(int addr) {
		pc = addr & 0xFFFF;
	}

	public int getSP() {
		return registers[15];
	}

	public void setSP(int value) {
		registers[15] = value & 0xFFFF;
	}
}

package emulator01;

//Núcleo da CPU 68k
public class CPU68000 {
	private int[] registers = new int[16]; // D0-D7, A0-A7
	private int pc; // Program Counter
	private Memory memory;

	public CPU68000(Memory mem) {
		this.memory = mem;
		this.pc = mem.readWord(0); // Reset Vector
	}

	public void step() {
		int opcode = memory.readWord(pc);
		pc += 2;
		decodeAndExecute(opcode);
	}

	private void decodeAndExecute(int opcode) {
		switch (opcode & 0xF000) {
		case 0x1000: // Exemplo: MOVE
			 moveExample();
			break;
		case 0x2000: // Exemplo: ADD
			 addExample();
			break;
		default:
			System.out.println("Opcode não implementado: " + Integer.toHexString(opcode));
		}
	}

	public int getPC() {
		return pc;
	}

	private void moveExample() {
		System.out.println("Executando MOVE: movendo D0 -> D1");
		registers[1] = registers[0];
	}

	private void addExample() {
		System.out.println("Executando ADD: D0 + D1 -> D0");
		registers[0] += registers[1];
	}
}

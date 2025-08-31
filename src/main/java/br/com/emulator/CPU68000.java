package br.com.emulator;

//Núcleo da CPU 68k
public class CPU68000 {

	private int[] registers = new int[16]; // D0-D7, A0-A7
	private int pc; // Program Counter
	private Memory memory;

	public CPU68000(Memory mem) {
		this.memory = mem;
		this.pc = memory.readWord(0); // Reset Vector (simplified)
	}

	public void step() {
		try {
			int opcode = memory.readWord(pc);
			pc += 2;
			// Very small demo: treat opcode 0x4E71 as NOP (RTS etc. actual 68k decoding is
			// huge)
			if (opcode == 0x4E71) {
				// NOP
			} else {
				// For debugging, we just print unknown opcodes occasionally
				// System.out.println("Opcode: " + Integer.toHexString(opcode));
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			// End of ROM / invalid PC - wrap for demo
			pc = 0;
		}
	}

	public int getPC() {
		return pc;
	}

}

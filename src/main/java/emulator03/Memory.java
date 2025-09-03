package emulator03;

public class Memory {

	private byte[] ram;

	public Memory(Cartridge cart) {
		// ROM + RAM simples (para estudo)
		ram = new byte[cart.getSize() + 0x10000];
		System.arraycopy(cart.getROMData(), 0, ram, 0, cart.getSize());
	}

	// --- Leitura ---
	public int readByte(int addr) {
		return ram[addr & 0xFFFFFF] & 0xFF;
	}

	public int readWord(int addr) {
		int hi = readByte(addr);
		int lo = readByte(addr + 1);
		return (hi << 8) | lo;
	}

	public int readLong(int addr) {
		int hi = readWord(addr);
		int lo = readWord(addr + 2);
		return (hi << 16) | lo;
	}

	// --- Escrita ---
	public void writeByte(int addr, int value) {
		ram[addr & 0xFFFFFF] = (byte) (value & 0xFF);
	}

	public void writeWord(int addr, int value) {
		writeByte(addr, (value >> 8) & 0xFF);
		writeByte(addr + 1, value & 0xFF);
	}

	public void writeLong(int addr, int value) {
		writeWord(addr, (value >> 16) & 0xFFFF);
		writeWord(addr + 2, value & 0xFFFF);
	}

}

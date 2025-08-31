package br.com.emulator;

//Barramento / RAM / ROM
public class Memory {

	private byte[] ram = new byte[64 * 1024]; // 64KB RAM (placeholder)
	private Cartridge rom;

	public Memory(Cartridge rom) {
		this.rom = rom;
	}

	public int readWord(int address) {
		int high = (readByte(address) & 0xFF);
		int low = (readByte(address + 1) & 0xFF);
		return (high << 8) | low;
	}

	public byte readByte(int address) {
		if (rom != null && address >= 0 && address < rom.getSize()) {
			return rom.read(address);
		}
		// mirror into RAM region for demo
		int idx = Math.floorMod(address, ram.length);
		return ram[idx];
	}

	public void writeByte(int address, byte value) {
		int idx = Math.floorMod(address, ram.length);
		ram[idx] = value;
	}

}

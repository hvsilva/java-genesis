package emulator01;

// Barramento / RAM / ROM
public class Memory {
	private byte[] ram = new byte[64 * 1024]; // 64KB RAM
    private Cartridge rom;

    public Memory(Cartridge rom) {
        this.rom = rom;
    }

    public int readWord(int address) {
        int high = readByte(address) & 0xFF;
        int low = readByte(address + 1) & 0xFF;
        return (high << 8) | low;
    }

    public byte readByte(int address) {
        if (address < rom.getSize()) {
            return rom.read(address);
        } else {
            return ram[address % ram.length];
        }
    }

    public void writeByte(int address, byte value) {
        if (address < ram.length) {
            ram[address] = value;
        }
    }
}

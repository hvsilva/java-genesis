package emulator03;

public class Memory {

	private byte[] rom;
    private int[] vram = new int[320 * 224]; // VRAM para pixels

    public Memory(Cartridge cart) {
        this.rom = cart.getROMData();
    }

    public int readWord(int addr) {
        return ((rom[addr] & 0xFF) << 8) | (rom[addr + 1] & 0xFF);
    }

    public void writeWord(int addr, int value) {
        if (addr >= 0x1000 && addr < 0x1000 + vram.length) { // VRAM mapeada a partir de 0x1000
            vram[addr - 0x1000] = value;
        } else {
            rom[addr] = (byte)((value >> 8) & 0xFF);
            rom[addr + 1] = (byte)(value & 0xFF);
        }
    }

    public int[] getVRAM() {
        return vram;
    }
}

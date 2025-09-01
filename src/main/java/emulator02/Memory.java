package emulator02;

public class Memory {
	
	 private byte[] rom;

	    public Memory(Cartridge cart) {
	        this.rom = cart.getROMData();
	    }

	    public byte readByte(int address) {
	        if (address < rom.length) return rom[address];
	        return 0;
	    }

	    public int readWord(int address) {
	        return ((readByte(address) & 0xFF) << 8) | (readByte(address + 1) & 0xFF);
	    }

}

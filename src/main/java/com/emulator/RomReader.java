package com.emulator;

/**
 * Classe utilitária para leitura de ROM
 */
public class RomReader {
	

    /**
     * Função genérica compatível com o emulador
     */
    public static long safeReadBytes(byte[] rom, long address, Size size) {
        switch (size) {
            case BYTE: return readCartridgeByte(address, rom);
            case WORD: return readCartridgeWord(address, rom);
            case LONG: return readCartridgeLong(address, rom);
            default:
                throw new IllegalArgumentException("Tamanho inválido: " + size);
        }
    }

	 /**
     * Lê um byte da ROM, com wrapping
     */
    public static long readCartridgeByte(long address, byte[] rom) {
        while (address >= rom.length) {
            address -= rom.length; // wrapping
        }
        return rom[(int) address]; // note: byte assinado para compatibilidade
    }

    /**
     * Lê uma palavra (WORD, 16 bits), compatível com array instructions[]
     * Mantém comportamento legacy: byte alto NÃO mascarado
     */
    public static long readCartridgeWord(long address, byte[] rom) {
    	   while (address >= rom.length) {
    	        address -= rom.length; // wrapping
    	    }
    	    int high = rom[(int) address] & 0xFF;
    	    int low  = rom[(int) address + 1] & 0xFF;
    	    return (high << 8) | low;  // sempre 0..65535
    }

    /**
     * Lê um valor LONG (32 bits), compatível com array instructions[]
     */
    public static long readCartridgeLong(long address, byte[] rom) {
        while (address >= rom.length) {
            address -= rom.length;
        }
        long data = (rom[(int) address] << 24)
                  | ((rom[(int) address + 1] & 0xFF) << 16)
                  | ((rom[(int) address + 2] & 0xFF) << 8)
                  | (rom[(int) address + 3] & 0xFF);
        return data;
    }
}

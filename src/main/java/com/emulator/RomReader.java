package com.emulator;

/**
 * Classe utilitária para leitura de ROM
 */
public class RomReader {	

    /**
     * Função genérica compatível com o emulador
     */
	public static long safeReadBytes(int[] rom, long address, Size size) {
	    int offset = (int) address;
	    switch (size) {
	        case BYTE:
	            return rom[offset] & 0xFF;

	        case WORD:
	            return ((rom[offset] & 0xFF) << 8) |
	                   (rom[offset + 1] & 0xFF);

	        case LONG:
	            return ((rom[offset] & 0xFF) << 24) |
	                   ((rom[offset + 1] & 0xFF) << 16) |
	                   ((rom[offset + 2] & 0xFF) << 8) |
	                   (rom[offset + 3] & 0xFF);

	        default:
	            return 0;
	    }
	}
}

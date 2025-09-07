package com.emulator;

import java.io.IOException;

public class MainTest {

	public static void main(String[] args) throws IOException {
		
		Cartridge cartridge = new Cartridge("D:\\PROJETOS_EMULADOR\\java-genesis\\roms\\rom_test.bin");
		
		Memory memory = new Memory(cartridge);
		CPU68000 cpu = new CPU68000(memory);

		// Roda alguns ciclos
		for (int i = 0; i < 20; i++) {
//		    cpu.step();
		}

	}

}
